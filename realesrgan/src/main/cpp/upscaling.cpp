//
// Created by Zhenxiang Chen on 04/01/23.
//

#include "upscaling.h"

#include <chrono>
#include "unsupported/Eigen/CXX11/Tensor"

#include "coroutine_utils.h"
#include "image_tile_interpreter.h"
#include "progress_tracker.h"

void pixels_matrix_to_float_array(const Eigen::MatrixXi& tile, float* float_buffer) {

    // Convert input image RGB int array to float array
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, tile height, tile width]
    const int tile_size = tile.size();
    const int* tile_data = tile.data();
    const int green_start_index = tile_size;
    const int blue_start_index = tile_size * 2;
    for (int i = 0; i < tile_size; i++) {
        // Alpha is ignored
        float_buffer[i] = (float)((tile_data[i] >> 16) & 0xff) / 255.0;
        float_buffer[i + green_start_index] = (float)((tile_data[i] >> 8) & 0xff) / 255.0;
        float_buffer[i + blue_start_index] = (float)((tile_data[i]) & 0xff) / 255.0;
    }
}

Eigen::MatrixXi output_tensor_to_pixels_matrix(const Eigen::Tensor<float, 3, Eigen::RowMajor>* tensor) {

    const float* tensor_data = tensor->data();
    const size_t pixels_count = tensor->size() / REALESRGAN_IMAGE_CHANNELS;
    int pixel_channels[REALESRGAN_IMAGE_CHANNELS];
    int output_tile_rgb[pixels_count];
    for (int i = 0; i < pixels_count; i++) {
        for (int j = 0; j < REALESRGAN_IMAGE_CHANNELS; j++) {
            pixel_channels[j] = std::max<float>(
                    0, std::min<float>(255, tensor_data[i + j * pixels_count] * 255));
        }

        // When we have RGB values, we pack them into output_tile single pixel.
        // Alpha is set to 255.
        output_tile_rgb[i] = (255u & 0xff) << 24 | (pixel_channels[0] & 0xff) << 16 |
                             (pixel_channels[1] & 0xff) << 8 |
                             (pixel_channels[2] & 0xff);
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
    if (position == 0) {
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

Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* process_tiles(
        JNIEnv* jni_env,
        jobject progress_tracker,
        jobject coroutine_scope,
        const ImageTileInterpreter& interpreter,
        const Eigen::MatrixXi& image_matrix,
        const int scale,
        const int padding) {

    const auto start = std::chrono::high_resolution_clock::now();

    const int tile_size = interpreter.tile_size;
    const int height = image_matrix.rows();
    const int width = image_matrix.cols();
    const auto output_image_matrix = new Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>(height * scale, width * scale);

    int y = 0;
    int last_row_height = 0;
    int processed_pixels = 0;
    set_progress_percentage(jni_env, progress_tracker, 0);

    while (is_coroutine_scope_active(jni_env, coroutine_scope)) {
        int x = 0;
        std::pair<int, int> y_padding = calculate_axis_padding(y, height, tile_size, padding);

        while (is_coroutine_scope_active(jni_env, coroutine_scope)) {

            std::pair<int, int> x_padding = calculate_axis_padding(x, width, tile_size, padding);

            // Get tile of pixels to process keeping, apply left padding as offset that will be cropped later
            Eigen::MatrixXi tile = image_matrix.block(y - y_padding.first, x - x_padding.first, tile_size, tile_size);

            // Feed input into tensor
            pixels_matrix_to_float_array(tile, interpreter.input_buffer);

            // Run inference on the model
            interpreter.inference();

            // Read MNN tensor as Eigen tensor
            const int output_tile_size = tile_size * scale;
            const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> output_tile(
                    interpreter.output_buffer,
                    REALESRGAN_IMAGE_CHANNELS,
                    output_tile_size,
                    output_tile_size);

            const Eigen::Tensor<float, 3, Eigen::RowMajor> cropped_output_tile = trim_tensor_padding(
                    scale,
                    x_padding,
                    y_padding,
                    &output_tile);

            const Eigen::MatrixXi tile_rgb_matrix = output_tensor_to_pixels_matrix(&cropped_output_tile);

            output_image_matrix->block(
                    y * scale,
                    x * scale,
                    tile_rgb_matrix.rows(),
                    tile_rgb_matrix.cols()) = tile_rgb_matrix;

            // Update progress
            processed_pixels += tile_rgb_matrix.size();
            const float progress = 100 * ((float)processed_pixels / output_image_matrix->size());
            // Calculate execution time per 1%
            const double percentage_execution_millis = std::chrono::duration<double, std::milli>(
                    std::chrono::high_resolution_clock::now() - start).count() / progress;
            set_progress_percentage(
                    jni_env,
                    progress_tracker,
                    progress,
                    lround((100 - progress) * percentage_execution_millis));

            // Recalculate padding and position of next tile in row
            if (x_padding.second == 0) {
                break;
            } else {
                x += tile_rgb_matrix.cols() / scale;
                last_row_height = tile_rgb_matrix.rows() / scale;
            }
        }

        // Recalculate padding and position of next column's tiles
        if (y_padding.second == 0) {
            break;
        } else {
            y += last_row_height;
        }
    }

    return output_image_matrix;
}

const Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* run_inference(
        JNIEnv* jni_env,
        jobject progress_tracker,
        jobject coroutine_scope,
        const mnn_model* model,
        int scale,
        const Eigen::MatrixXi& input_image) {

    const auto interpreter = ImageTileInterpreter(model, REALESRGAN_INPUT_TILE_SIZE);

    const auto output_image = process_tiles(
            jni_env,
            progress_tracker,
            coroutine_scope,
            interpreter,
            input_image,
            scale,
            REALESRGAN_INPUT_TILE_PADDING);

    return output_image;
}
