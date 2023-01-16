//
// Created by Zhenxiang Chen on 04/01/23.
//

#include <algorithm>
#include <array>
#include <cmath>
#include <memory>

#include "MNN/Interpreter.hpp"

#include "unsupported/Eigen/CXX11/Tensor"

#include "realesrgan.h"

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
    } else if (axis_size - position - padding <= tile_size) {
        // Final tile
        return std::pair<int, int> {(tile_size - (axis_size - position)), 0};
    } else {
        // Tiles in between
        return std::pair<int, int> {padding, padding};
    }
}

Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* process_tiles(
        MNN::Interpreter* interpreter,
        MNN::Session* session,
        const Eigen::MatrixXi& image_matrix,
        const int scale,
        const int tile_size,
        const int padding) {

    const int height = image_matrix.rows();
    const int width = image_matrix.cols();
    const auto output_image_matrix = new Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>(height * scale, width * scale);

    MNN::Tensor* interpreter_input = interpreter->getSessionInput(session, "input.1");
    interpreter->resizeTensor(interpreter_input, 1, REALESRGAN_IMAGE_CHANNELS, tile_size, tile_size);
    interpreter->resizeSession(session);
    MNN::Tensor* interpreter_output = interpreter->getSessionOutput(session, "1895");
    MNN::Tensor input_tensor(interpreter_input, MNN::Tensor::CAFFE);
    MNN::Tensor output_tensor(interpreter_output, MNN::Tensor::CAFFE);
    auto input_tensor_buffer = input_tensor.host<float>();
    auto output_tensor_buffer = output_tensor.host<float>();

    int y = 0;
    int last_row_height = 0;
    while (true) {
        int x = 0;
        std::pair<int, int> y_padding = calculate_axis_padding(y, height, tile_size, padding);
        y -= y_padding.first;
        const bool final_y = y_padding.second == 0;

        while (true) {
            std::pair<int, int> x_padding = calculate_axis_padding(x, width, tile_size, padding);
            const bool final_x = x_padding.second == 0;

            Eigen::MatrixXi tile = image_matrix.block(y - y_padding.first, x - x_padding.first, tile_size, tile_size);

            // Feed input into tensor
            pixels_matrix_to_float_array(tile, input_tensor_buffer);

            // Feed data to the interpreter
            interpreter_input->copyFromHostTensor(&input_tensor);

            // Run the interpreter
            interpreter->runSession(session);

            // Extract result from interpreter
            interpreter_output->copyToHostTensor(&output_tensor);

            // Read MNN tensor as Eigen tensor
            const int output_tile_size = tile_size * scale;
            const Eigen::TensorMap<Eigen::Tensor<float, 3, Eigen::RowMajor>> output_tile(
                    output_tensor_buffer,
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

            // Recalculate padding and position of next tile in row
            if (final_x) {
                break;
            } else {
                x += tile_rgb_matrix.cols() / scale;
                last_row_height = tile_rgb_matrix.rows() / scale;
            }
        }

        // Recalculate padding and position of next column's tiles
        if (final_y) {
            break;
        } else {
            y += last_row_height;
        }
    }

    return output_image_matrix;
}

const Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* run_inference(
        const void* model_data,
        const long model_size,
        int scale,
        const Eigen::MatrixXi& input_image) {

    MNN::Interpreter* interpreter = MNN::Interpreter::createFromBuffer(model_data, model_size);

    if (!interpreter) {
        LOGE("Failed to create MNN interpreter");
        return nullptr;
    }

    MNN::ScheduleConfig config;
    MNN::BackendConfig backendConfig;
    backendConfig.memory = MNN::BackendConfig::Memory_High;
    backendConfig.power = MNN::BackendConfig::Power_High;
    backendConfig.precision = MNN::BackendConfig::Precision_Low;
    config.backendConfig = &backendConfig;
    config.type = MNN_FORWARD_VULKAN;

    MNN::Session* session = interpreter->createSession(config);

    const auto output_image = process_tiles(
            interpreter,
            session,
            input_image,
            scale,
            REALESRGAN_INPUT_TILE_SIZE,
            REALESRGAN_INPUT_TILE_PADDING);

    // Cleanup
    interpreter->releaseSession(session);
    delete interpreter;

    return output_image;
}
