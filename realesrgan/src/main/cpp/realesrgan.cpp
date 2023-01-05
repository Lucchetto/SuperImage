//
// Created by Zhenxiang Chen on 04/01/23.
//

#include <algorithm>
#include <array>
#include <cmath>
#include <memory>

#include "tensorflow/lite/c/c_api.h"
#include "tensorflow/lite/c/c_api_experimental.h"

#include "realesrgan.h"

const output_image* run_inference(const void* model_data, const long model_size, int scale, const int* input_image) {

    // Load the model
    TfLiteModel* model = TfLiteModelCreate(model_data, model_size);
    if (!model) {
        LOGE("Failed to create TFLite model");
        return nullptr;
    }

    // Create the interpreter options
    TfLiteInterpreterOptions* options = TfLiteInterpreterOptionsCreate();
    TfLiteInterpreterOptionsSetUseNNAPI(options, true);

    TfLiteInterpreter* interpreter = TfLiteInterpreterCreate(model, options);
    if (!interpreter) {
        LOGE("Failed to create TFLite interpreter");
        TfLiteInterpreterOptionsDelete(options);
        TfLiteModelDelete(model);
        return nullptr;
    }

    // Allocate tensors and populate the input tensor data
    TfLiteStatus status = TfLiteInterpreterAllocateTensors(interpreter);
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when allocating tensors");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
        TfLiteModelDelete(model);
        return nullptr;
    }
    TfLiteTensor* input_tensor = TfLiteInterpreterGetInputTensor(interpreter, 0);

    // Convert input image RGB int array to float array
    // with tensor shape [1, REALESRGAN_IMAGE_CHANNELS, REALESRGAN_INPUT_IMAGE_HEIGHT, REALESRGAN_INPUT_IMAGE_WIDTH]
    float input_buffer[REALESRGAN_INPUT_IMAGE_TENSOR_SIZE];
    int green_start_index = REALESRGAN_INPUT_IMAGE_PIXELS;
    int blue_start_index = REALESRGAN_INPUT_IMAGE_PIXELS * 2;
    for (int i = 0; i < REALESRGAN_INPUT_IMAGE_PIXELS; i++) {
        // Alpha is ignored
        input_buffer[i] = (float)((input_image[i] >> 16) & 0xff) / 255.0;
        input_buffer[i + green_start_index] = (float)((input_image[i] >> 8) & 0xff) / 255.0;
        input_buffer[i + blue_start_index] = (float)((input_image[i]) & 0xff) / 255.0;
    }

    // Feed input into model
    status = TfLiteTensorCopyFromBuffer(
            input_tensor, input_buffer,
            REALESRGAN_INPUT_IMAGE_TENSOR_SIZE * sizeof(float));
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when copying input buffer to input tensor");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
        TfLiteModelDelete(model);
        return nullptr;
    }

    // Run the interpreter
    status = TfLiteInterpreterInvoke(interpreter);
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when running the TFLite model");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
        TfLiteModelDelete(model);
        return nullptr;
    }

    // Extract the output tensor data
    const TfLiteTensor* output_tensor =
            TfLiteInterpreterGetOutputTensor(interpreter, 0);
    const int output_tensor_pixels = REALESRGAN_INPUT_IMAGE_PIXELS * pow(scale, 2);
    const int output_tensor_size = output_tensor_pixels * REALESRGAN_IMAGE_CHANNELS;
    float output_buffer[output_tensor_size];
    status = TfLiteTensorCopyToBuffer(
            output_tensor, output_buffer,
            output_tensor_size * sizeof(float));
    if (status != kTfLiteOk) {
        LOGE("Something went wrong when copying output tensor to output buffer");
        TfLiteInterpreterDelete(interpreter);
        TfLiteInterpreterOptionsDelete(options);
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
    TfLiteModelDelete(model);

    return new output_image {
            .data = output_image_pixels,
            .size = output_tensor_pixels
    };
}
