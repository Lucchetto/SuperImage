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

#include "realesrgan.h"

float_ptr_array tile_to_float_array(const Eigen::Tensor<int, 2>& tile) {

    // Convert input image RGB int array to float array
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, tile height, tile width]
    const int tile_size = tile.size();
    const int* tile_data = tile.data();
    const size_t tensor_size = tile_size * REALESRGAN_IMAGE_CHANNELS;
    const auto float_buffer = (float *) malloc(sizeof(float) * tensor_size);
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
        .size = tensor_size,
    };
}

int* process_tiles(
        TfLiteInterpreter* interpreter,
        const Eigen::TensorMap<Eigen::Tensor<int, 2>> image_tensor,
        const int scale,
        const int tile_size,
        const int pre_padding) {

    const int height = image_tensor.dimension(0);
    const int width = image_tensor.dimension(1);
    const int offset_step = tile_size - pre_padding;

    for (int y = 0; y < height; y += offset_step) {
        for (int x = 0; x < width; x += offset_step) {
            const Eigen::array<Eigen::Index, 2> offsets = {y, x};

            const bool final_row_tile = x + offset_step > width;
            const bool final_column_tile = y + offset_step > height;

            const Eigen::array<Eigen::Index, 2> extents = {
                    final_column_tile ? (height - y) : tile_size,
                    final_row_tile ? (width - x) : tile_size
            };
            Eigen::Tensor<int, 2> tile;
            if (final_row_tile || final_column_tile) {
                // Pad sliced tensor to fit tile_size for model input
                Eigen::array<Eigen::Pair<int, int>, 2> padding;
                padding[0] = Eigen::Pair<int, int>(0, tile_size - extents[0]);
                padding[1] = Eigen::Pair<int, int>(0, tile_size - extents[1]);
                tile = image_tensor.slice(offsets, extents).pad(padding);
            } else {
                tile = image_tensor.slice(offsets, extents);
            }
            const float_ptr_array model_input = tile_to_float_array(tile);

            // Feed input into model
            if (TfLiteTensorCopyFromBuffer(
                    TfLiteInterpreterGetInputTensor(interpreter, 0),
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
            const size_t output_tensor_pixels = tile.size() * pow(scale, 2);
            const size_t output_tensor_size = output_tensor_pixels * REALESRGAN_IMAGE_CHANNELS;
            float output_buffer[output_tensor_size];
            if (TfLiteTensorCopyToBuffer(
                    TfLiteInterpreterGetOutputTensor(interpreter, 0),
                    output_buffer,
                    output_tensor_size * sizeof(float)) != kTfLiteOk) {
                LOGE("Something went wrong when copying output tensor to output buffer");
                free(model_input.data);
                return nullptr;
            }

            //LOGI("Test");
        }
    }

    return nullptr;
}

const output_image_t* run_inference(
        const void* model_data,
        const long model_size,
        int scale,
        const Eigen::TensorMap<Eigen::Tensor<int, 2>> input_image) {

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

    process_tiles(interpreter, input_image, scale, REALESRGAN_INPUT_TILE_SIZE, REALESRGAN_INPUT_TILE_PADDING);

    TfLiteTensor* model_input_tensor = TfLiteInterpreterGetInputTensor(interpreter, 0);

    const Eigen::array<Eigen::Index, 2> offsets = {64, 64};
    const Eigen::array<Eigen::Index, 2> extents = {64, 64};
    const Eigen::Tensor<int, 2> cropped = input_image.slice(offsets, extents);
    // Treat tensor as an one-dimensional array for simplicity
    const int input_image_size = cropped.size();
    const int* input_image_data = cropped.data();

    // Convert input image RGB int array to float array
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, REALESRGAN_INPUT_IMAGE_HEIGHT, REALESRGAN_INPUT_IMAGE_WIDTH]
    float input_buffer[input_image_size * REALESRGAN_IMAGE_CHANNELS];
    int green_start_index = input_image_size;
    int blue_start_index = input_image_size * 2;
    for (int i = 0; i < input_image_size; i++) {
        // Alpha is ignored
        input_buffer[i] = (float)((input_image_data[i] >> 16) & 0xff) / 255.0;
        input_buffer[i + green_start_index] = (float)((input_image_data[i] >> 8) & 0xff) / 255.0;
        input_buffer[i + blue_start_index] = (float)((input_image_data[i]) & 0xff) / 255.0;
    }

    // Feed input into model
    status = TfLiteTensorCopyFromBuffer(model_input_tensor, input_buffer, sizeof(input_buffer));
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when copying input buffer to input tensor");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
        TfLiteNnapiDelegateDelete(nnapi_delegate);
        TfLiteModelDelete(model);
        return nullptr;
    }

    // Run the interpreter
    status = TfLiteInterpreterInvoke(interpreter);
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when running the TFLite model");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
        TfLiteNnapiDelegateDelete(nnapi_delegate);
        TfLiteModelDelete(model);
        return nullptr;
    }

    // Extract the output tensor data
    const TfLiteTensor* output_tensor =
            TfLiteInterpreterGetOutputTensor(interpreter, 0);
    const size_t output_tensor_pixels = input_image_size * pow(scale, 2);
    const size_t output_tensor_size = output_tensor_pixels * REALESRGAN_IMAGE_CHANNELS;
    float output_buffer[output_tensor_size];
    status = TfLiteTensorCopyToBuffer(
            output_tensor, output_buffer,
            output_tensor_size * sizeof(float));
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when copying output tensor to output buffer");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
        TfLiteNnapiDelegateDelete(nnapi_delegate);
        TfLiteModelDelete(model);
        return nullptr;
    }

    // Postprocess the output from TFLite
    int pixel_channels[REALESRGAN_IMAGE_CHANNELS];
    int* output_image_pixels = (int *) malloc(sizeof(int) * output_tensor_pixels);
    for (int i = 0; i < output_tensor_pixels; i++) {
        for (int j = 0; j < REALESRGAN_IMAGE_CHANNELS; j++) {
            pixel_channels[j] = std::max<float>(
                    0, std::min<float>(255, output_buffer[i + j * output_tensor_pixels] * 255));
        }
        // When we have RGB values, we pack them into a single pixel.
        // Alpha is set to 255.
        output_image_pixels[i] = (255u & 0xff) << 24 | (pixel_channels[0] & 0xff) << 16 |
                                 (pixel_channels[1] & 0xff) << 8 |
                                 (pixel_channels[2] & 0xff);
    }

    // Cleanup
    TfLiteInterpreterDelete(interpreter);
    TfLiteInterpreterOptionsDelete(options);
    TfLiteNnapiDelegateDelete(nnapi_delegate);
    TfLiteModelDelete(model);

    return new output_image_t {
            .data = output_image_pixels,
            .size = output_tensor_pixels
    };
}
