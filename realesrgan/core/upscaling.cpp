//
// Created by Zhenxiang Chen on 04/01/23.
//

#include "upscaling.h"

#include <chrono>
#include "unsupported/Eigen/CXX11/Tensor"

#include "jni_common/coroutine_utils.h"
#include "jni_common/progress_tracker.h"
#include "image_tile_interpreter.h"

void pixels_matrix_to_float_array(const Eigen::MatrixXi& tile, float* float_buffer) {

    // Convert input image RGB int array to float array
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, tile height, tile width]
    const int tile_size = tile.size();
    const int* tile_data = tile.data();
    const int green_start_index = tile_size;
    const int blue_start_index = tile_size * 2;
    for (int i = 0; i < tile_size; i++) {
        // Alpha is ignored
        float_buffer[i] = (float)((tile_data[i]) & 0xff) / 255.0;
        float_buffer[i + green_start_index] = (float)((tile_data[i] >> 8) & 0xff) / 255.0;
        float_buffer[i + blue_start_index] = (float)((tile_data[i] >> 16) & 0xff) / 255.0;;
    }
}

Eigen::MatrixXi output_tensor_to_pixels_matrix(const Eigen::Tensor<float, 3, Eigen::RowMajor>* tensor) {

    uint8_t rgba_buffer[REALESRGAN_IMAGE_CHANNELS + 1] = {};
    // Alpha is always 255
    rgba_buffer[0] = 255;

    const float* tensor_data = tensor->data();
    const size_t pixels_count = tensor->size() / REALESRGAN_IMAGE_CHANNELS;
    int output_tile_rgb[pixels_count];
    for (int i = 0; i < pixels_count; i++) {
        for (int j = 0; j < REALESRGAN_IMAGE_CHANNELS; j++) {
            rgba_buffer[j + 1] = std::clamp<float>(tensor_data[i + j * pixels_count] * 255, 0, 255);
        }

        // When we have RGB values, we pack them into output_tile single pixel in RGBA.
        // Assume little endian order since this will only run on ARM and x86
        output_tile_rgb[i] = (rgba_buffer[0] & 0xff) << 24 | (rgba_buffer[3] & 0xff) << 16 |
                             (rgba_buffer[2] & 0xff) << 8 |
                             (rgba_buffer[1] & 0xff);
    }

    return Eigen::Map<Eigen::MatrixXi>(
            output_tile_rgb,
            tensor->dimension(2),
            tensor->dimension(1));
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
        const Eigen::MatrixXi& input_image_matrix,
        Eigen::Map<Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>& output_image_matrix,
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

    const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> output_tile(
            interpreter.output_buffer,
            REALESRGAN_IMAGE_CHANNELS,
            tile_dimensions->width * scale,
            tile_dimensions->height * scale);

    while (is_coroutine_scope_active(jni_env, coroutine_scope)) {
        int x = 0;
        std::pair<int, int> y_padding = calculate_axis_padding(y, height, tile_dimensions->height, padding);

        while (is_coroutine_scope_active(jni_env, coroutine_scope)) {

            std::pair<int, int> x_padding = calculate_axis_padding(x, width, tile_dimensions->width, padding);

            // Get tile of pixels to process keeping, apply left padding as offset that will be cropped later
            Eigen::MatrixXi tile = input_image_matrix.block(
                    y - y_padding.first,
                    x - x_padding.first,
                    tile_dimensions->height,
                    tile_dimensions->width);

            // Feed input into tensor
            pixels_matrix_to_float_array(tile, interpreter.input_buffer);

            // Run inference on the model
            interpreter.inference();

            const Eigen::Tensor<float, 3, Eigen::RowMajor> cropped_output_tile = trim_tensor_padding(
                    scale,
                    x_padding,
                    y_padding,
                    &output_tile);

            const Eigen::MatrixXi tile_rgb_matrix = output_tensor_to_pixels_matrix(&cropped_output_tile);

            output_image_matrix.block(
                    y * scale,
                    x * scale,
                    tile_rgb_matrix.rows(),
                    tile_rgb_matrix.cols()) = tile_rgb_matrix;

            // Update progress
            processed_pixels += tile_rgb_matrix.size();
            const float progress = 100 * ((float)processed_pixels / output_image_matrix.size());
            // Calculate execution time per 1%
            const double percentage_execution_millis = std::chrono::duration<double, std::milli>(
                    std::chrono::high_resolution_clock::now() - start).count() / progress;
            set_progress_percentage(
                    jni_env,
                    progress_tracker,
                    progress,
                    lround((100 - progress) * percentage_execution_millis));

            // Recalculate padding and position of next tile in row
            x += tile_rgb_matrix.cols() / scale;
            if (x == width) {
                last_row_height = tile_rgb_matrix.rows() / scale;
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
        const Eigen::MatrixXi& input_image_matrix,
        Eigen::Map<Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>& output_image_matrix) {

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
