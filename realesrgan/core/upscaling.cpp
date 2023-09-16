//
// Created by Zhenxiang Chen on 04/01/23.
//

#include "upscaling.h"

#include <chrono>
#include "unsupported/Eigen/CXX11/Tensor"

#include "jni_common/coroutine_utils.h"
#include "jni_common/progress_tracker.h"
#include "image_tile_interpreter.h"

void pixels_matrix_to_float_array(const Eigen::Block<const PixelMatrix>& tile,
                                  const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>& tensor) {

    // Convert input image RGB int array to float array
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, tile height, tile width]
    for (int y = 0; y < tile.rows(); y++) {
        for (int x = 0; x < tile.cols(); x++) {
            // Alpha is ignored
            const int32_t pixel = tile(y, x);
            tensor(0, x, y) = (float)(pixel & 0xff) / 255.0;
            tensor(1, x, y) = (float)((pixel >> 8) & 0xff) / 255.0;
            tensor(2, x, y) = (float)((pixel >> 16) & 0xff) / 255.0;
        }
    }
}

void output_tensor_to_pixels_matrix(
        Eigen::Block<PixelMatrix>& matrix,
        const Eigen::Tensor<float, 3, Eigen::RowMajor>& tensor) {

    for (int y = 0; y < tensor.dimension(2); y++) {
        for (int x = 0; x < tensor.dimension(1); x++) {
            // When we have RGB values, we pack them into output_tile single pixel in RGBA.
            // Assume little endian order since this will only run on ARM and x86
            const uint8_t r = std::clamp<float>(tensor(2, x, y) * 255, 0, 255);
            const uint8_t g = std::clamp<float>(tensor(1, x, y) * 255, 0, 255);
            const uint8_t b = std::clamp<float>(tensor(0, x, y) * 255, 0, 255);
            matrix(y, x) = (255 & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
        }
    }
}

Eigen::Tensor<float, 3, Eigen::RowMajor> trim_tensor_padding(
        const int scale,
        const std::pair<int, int> x_padding,
        const std::pair<int, int> y_padding,
        const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>>* tensor) {

    Eigen::array<Eigen::Index, 3> offsets = {
            0,
            x_padding.first * scale,
            y_padding.first * scale};
    Eigen::array<Eigen::Index, 3> extents = {
            tensor->dimension(0),
            tensor->dimension(1) - x_padding.first * scale - x_padding.second * scale,
            tensor->dimension(2) - y_padding.first * scale - y_padding.second * scale};

    return tensor->slice(offsets, extents);
}

std::pair<int, int> calculate_axis_padding(const int position, const int axis_size, const int tile_size, const int padding) {
    if (axis_size == tile_size) {
        // No padding needed if there a single tile for given axis
        return std::pair<int, int> {0, 0};
    } else if (position == 0) {
        // First tile
        return std::pair<int, int> {0, padding};
    } else if (axis_size - position <= tile_size - padding) {
        // Final tile
        return std::pair<int, int> {(tile_size - (axis_size - position)), 0};
    } else {
        // Tiles in between
        return std::pair<int, int> {padding, padding};
    }
}

void process_tiles(
        JNIEnv* jni_env,
        jobject progress_tracker,
        jobject coroutine_scope,
        const ImageTileInterpreter& interpreter,
        const PixelMatrix& input_image_matrix,
        PixelMatrix& output_image_matrix,
        const int scale,
        const int padding) {

    const auto start = std::chrono::high_resolution_clock::now();

    int y = 0;
    int last_row_height = 0;
    int processed_pixels = 0;
    set_progress_percentage(jni_env, progress_tracker, 0);

    const image_dimensions* tile_dimensions = &interpreter.tile_dimensions;
    const int height = input_image_matrix.rows();
    const int width = input_image_matrix.cols();

    const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> input_tensor(
            interpreter.input_buffer,
            REALESRGAN_IMAGE_CHANNELS,
            tile_dimensions->width,
            tile_dimensions->height);

    const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> output_tensor(
            interpreter.output_buffer,
            REALESRGAN_IMAGE_CHANNELS,
            tile_dimensions->width * scale,
            tile_dimensions->height * scale);

    while (is_coroutine_scope_active(jni_env, coroutine_scope)) {
        int x = 0;
        std::pair<int, int> y_padding = calculate_axis_padding(y, height, tile_dimensions->height, padding);

        while (is_coroutine_scope_active(jni_env, coroutine_scope)) {

            std::pair<int, int> x_padding = calculate_axis_padding(x, width, tile_dimensions->width, padding);

            // Get input_tile of pixels to process keeping, apply left padding as offset that will be cropped later
            const Eigen::Block input_tile = input_image_matrix.block(
                    y - y_padding.first,
                    x - x_padding.first,
                    tile_dimensions->height,
                    tile_dimensions->width);

            // Feed input into tensor
            pixels_matrix_to_float_array(input_tile, input_tensor);

            // Run inference on the model
            interpreter.inference();

            const Eigen::Tensor<float, 3, Eigen::RowMajor> cropped_output_tensor = trim_tensor_padding(
                    scale,
                    x_padding,
                    y_padding,
                    &output_tensor);

            Eigen::Block<PixelMatrix> output_dest_block = output_image_matrix.block(
                    y * scale,
                    x * scale,
                    cropped_output_tensor.dimension(2),
                    cropped_output_tensor.dimension(1));

            output_tensor_to_pixels_matrix(output_dest_block, cropped_output_tensor);

            // Update progress
            processed_pixels += output_dest_block.size();
            const float progress = 100 * ((float)processed_pixels / output_image_matrix.size());
            // Calculate execution time per 1%
            const double percentage_execution_millis = std::chrono::duration<double, std::milli>(
                    std::chrono::high_resolution_clock::now() - start).count() / progress;
            set_progress_percentage(
                    jni_env,
                    progress_tracker,
                    progress,
                    static_cast<int64_t>(round((100 - progress) * percentage_execution_millis)));

            // Recalculate padding and position of next input_tile in row
            x += output_dest_block.cols() / scale;
            if (x == width) {
                last_row_height = output_dest_block.rows() / scale;
                break;
            }
        }

        y += last_row_height;
        // Recalculate padding and position of next column's tiles
        if (y == height) {
            break;
        }
    }
}

void run_inference(
        JNIEnv* jni_env,
        jobject progress_tracker,
        jobject coroutine_scope,
        const mnn_model* model,
        int scale,
        const PixelMatrix& input_image_matrix,
        PixelMatrix& output_image_matrix) {

    const auto interpreter = ImageTileInterpreter(
            model,
            {
                // Adapt tile size if image size is smaller than default tile size
                .width = std::min<int>(REALESRGAN_INPUT_TILE_SIZE, input_image_matrix.cols()),
                .height = std::min<int>(REALESRGAN_INPUT_TILE_SIZE, input_image_matrix.rows())
            });

    process_tiles(
            jni_env,
            progress_tracker,
            coroutine_scope,
            interpreter,
            input_image_matrix,
            output_image_matrix,
            scale,
            REALESRGAN_INPUT_TILE_PADDING);
}
