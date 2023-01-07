//
// Created by Zhenxiang Chen on 04/01/23.
//

#include <algorithm>
#include <array>
#include <cmath>
#include <memory>

#include "tensorflow/lite/c/c_api.h"
#include "tensorflow/lite/c/c_api_experimental.h"
#include "tensorflow/lite/delegates/nnapi/nnapi_delegate_c_api.h"

#include "unsupported/Eigen/CXX11/Tensor"

#include "realesrgan.h"

float_ptr_array tile_to_float_array(const Eigen::MatrixXi& tile) {

    // Convert input image RGB int array to float array
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, tile height, tile width]
    const int tile_size = tile.size();
    const int* tile_data = tile.data();
    const size_t float_buffer_size = tile_size * REALESRGAN_IMAGE_CHANNELS;
    const auto float_buffer = (float *) malloc(sizeof(float) * float_buffer_size);
    const int green_start_index = tile_size;
    const int blue_start_index = tile_size * 2;
    for (int i = 0; i < tile_size; i++) {
        // Alpha is ignored
        float_buffer[i] = (float)((tile_data[i] >> 16) & 0xff) / 255.0;
        float_buffer[i + green_start_index] = (float)((tile_data[i] >> 8) & 0xff) / 255.0;
        float_buffer[i + blue_start_index] = (float)((tile_data[i]) & 0xff) / 255.0;
    }

    return float_ptr_array {
        .data = float_buffer,
        .size = float_buffer_size,
    };
}

std::pair<int, int> calculate_tile_padding(const int position, const int axis_size, const int tile_size, const int padding) {
    if (position + tile_size >= axis_size || position == tile_size) {
        // Last tile or second tile of axis
        return std::pair<int, int> {padding, 0};
    } else {
        // Tiles in between
        return std::pair<int, int> {padding, padding};
    }
}

bool is_final_tile_in_axis(const std::pair<int, int> paddings) {
    return paddings.second == 0;
}

Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* process_tiles(
        TfLiteInterpreter* interpreter,
        const Eigen::MatrixXi& image_matrix,
        const int scale,
        const int tile_size,
        const int padding) {

    const int height = image_matrix.rows();
    const int width = image_matrix.cols();
    const auto output_image_matrix = new Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>(height * scale, width * scale);
    TfLiteTensor* input_tensor = TfLiteInterpreterGetInputTensor(interpreter, 0);
    const TfLiteTensor* output_tensor = TfLiteInterpreterGetOutputTensor(interpreter, 0);

    int y = 0;
    while (y < height) {
        const int y_from_end = height - y;
        const bool incomplete_y = y_from_end < tile_size;
        int x = 0;

        while (x < width) {
            const int x_from_end = width - x;
            const bool incomplete_x = x_from_end < tile_size;

            Eigen::MatrixXi tile = image_matrix.block(
                    incomplete_y ? (height - tile_size) : y,
                    incomplete_x ? (width - tile_size) : x,
                    tile_size,
                    tile_size);

            const float_ptr_array model_input = tile_to_float_array(tile);

            // Feed input into model
            if (TfLiteTensorCopyFromBuffer(
                    input_tensor,
                    model_input.data,
                    model_input.size * sizeof(float)) != kTfLiteOk) {
                LOGE("Something went wrong when copying input buffer to input tensor");
                free(model_input.data);
                return nullptr;
            }

            // Run the interpreter
            if (TfLiteInterpreterInvoke(interpreter) != kTfLiteOk) {
                LOGE("Something went wrong when running the TFLite model");
                free(model_input.data);
                return nullptr;
            }

            free(model_input.data);

            // Extract the output tensor data
            const int output_tile_size = tile_size * scale;
            Eigen::Tensor<float, 3, Eigen::RowMajor> output_tile(REALESRGAN_IMAGE_CHANNELS, output_tile_size, output_tile_size);
            if (TfLiteTensorCopyToBuffer(
                    output_tensor,
                    output_tile.data(),
                    output_tile.size() * sizeof(float)) != kTfLiteOk) {
                LOGE("Something went wrong when copying output tensor to output buffer");
                return nullptr;
            }

            Eigen::Tensor<float, 3, Eigen::RowMajor> cropped_output_tile;
            if (incomplete_x || incomplete_y) {
                // Slice off the part of tile that's already present in previous tile
                const int scaled_y_from_end = incomplete_y ? y_from_end * scale : output_tile_size;
                const int scaled_x_from_end = incomplete_x ? x_from_end * scale : output_tile_size;
                Eigen::array<Eigen::Index, 3> offsets = {
                        0,
                        output_tile_size - scaled_x_from_end,
                        output_tile_size - scaled_y_from_end};
                Eigen::array<Eigen::Index, 3> extents = {
                        REALESRGAN_IMAGE_CHANNELS,
                        scaled_x_from_end,
                        scaled_y_from_end};
                cropped_output_tile = output_tile.slice(offsets, extents);
            } else {
                cropped_output_tile = output_tile;
            }

            // Postprocess the output from TFLite
            const float* output_tile_data = cropped_output_tile.data();
            const size_t output_tile_pixels = cropped_output_tile.size() / REALESRGAN_IMAGE_CHANNELS;
            int pixel_channels[REALESRGAN_IMAGE_CHANNELS];
            int output_tile_rgb[output_tile_pixels];
            for (int i = 0; i < output_tile_pixels; i++) {
                for (int j = 0; j < REALESRGAN_IMAGE_CHANNELS; j++) {
                    pixel_channels[j] = std::max<float>(
                            0, std::min<float>(255, output_tile_data[i + j * output_tile_pixels] * 255));
                }
                // When we have RGB values, we pack them into output_tile single pixel.
                // Alpha is set to 255.
                output_tile_rgb[i] = (255u & 0xff) << 24 | (pixel_channels[0] & 0xff) << 16 |
                                     (pixel_channels[1] & 0xff) << 8 |
                                     (pixel_channels[2] & 0xff);
            }

            Eigen::Map<Eigen::MatrixXi> tile_rgb_matrix(
                    output_tile_rgb,
                    cropped_output_tile.dimension(2),
                    cropped_output_tile.dimension(1));

            output_image_matrix->block(
                    y * scale,
                    x * scale,
                    tile_rgb_matrix.rows(),
                    tile_rgb_matrix.cols()) = tile_rgb_matrix;

            // Recalculate padding and position of next tile in row
            if (incomplete_x) {
                break;
            } else {
                x += tile_size;
            }
        }

        // Recalculate padding and position of next column's tiles
        if (incomplete_y) {
            break;
        } else {
            y += tile_size;
        }
    }

    return output_image_matrix;
}

const Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* run_inference(
        const void* model_data,
        const long model_size,
        int scale,
        const Eigen::MatrixXi& input_image) {

    // Load the model
    TfLiteModel* model = TfLiteModelCreate(model_data, model_size);
    if (!model) {
        LOGE("Failed to create TFLite model");
        return nullptr;
    }

    // Create the interpreter options
    TfLiteInterpreterOptions* options = TfLiteInterpreterOptionsCreate();
    TfLiteNnapiDelegateOptions nnapi_options = TfLiteNnapiDelegateOptionsDefault();
    nnapi_options.allow_fp16 = true;
    nnapi_options.execution_preference = TfLiteNnapiDelegateOptions::kSustainedSpeed;
    TfLiteDelegate* nnapi_delegate = TfLiteNnapiDelegateCreate(&nnapi_options);
    if (!nnapi_delegate) {
        LOGE("Failed to create NNAPI delegate");
        TfLiteInterpreterOptionsDelete(options);
        TfLiteModelDelete(model);
        return nullptr;
    }

    TfLiteInterpreterOptionsAddDelegate(options, nnapi_delegate);
    TfLiteInterpreter* interpreter = TfLiteInterpreterCreate(model, options);
    if (!interpreter) {
        LOGE("Failed to create TFLite interpreter");
        TfLiteInterpreterOptionsDelete(options);
        TfLiteNnapiDelegateDelete(nnapi_delegate);
        TfLiteModelDelete(model);
        return nullptr;
    }

    // Allocate tensors and populate the input tensor data
    TfLiteStatus status = TfLiteInterpreterAllocateTensors(interpreter);
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when allocating tensors");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
        TfLiteNnapiDelegateDelete(nnapi_delegate);
        TfLiteModelDelete(model);
        return nullptr;
    }

    const auto output_image = process_tiles(
            interpreter,
            input_image,
            scale,
            REALESRGAN_INPUT_TILE_SIZE,
            REALESRGAN_INPUT_TILE_PADDING);

    // Cleanup
    TfLiteInterpreterDelete(interpreter);
    TfLiteInterpreterOptionsDelete(options);
    TfLiteNnapiDelegateDelete(nnapi_delegate);
    TfLiteModelDelete(model);

    return output_image;
}
