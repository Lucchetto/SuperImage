//
// Created by Zhenxiang Chen on 04/01/23.
//

#ifndef TFREALESRGAN_REALESRGAN_H
#define TFREALESRGAN_REALESRGAN_H

#include <android/log.h>

#define REALESRGAN_IMAGE_CHANNELS 3

#define REALESRGAN_INPUT_IMAGE_WIDTH 64
#define REALESRGAN_INPUT_IMAGE_HEIGHT 64
#define REALESRGAN_INPUT_IMAGE_PIXELS REALESRGAN_INPUT_IMAGE_WIDTH * REALESRGAN_INPUT_IMAGE_HEIGHT
#define REALESRGAN_INPUT_IMAGE_TENSOR_SIZE REALESRGAN_INPUT_IMAGE_PIXELS * REALESRGAN_IMAGE_CHANNELS

#define LOG_TAG "RealESRGAN"
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

struct output_image {
    int* data;
    const int size;
};

const output_image* run_inference(const void* model_data, const long model_size, int scale, const int* input_image);

#endif //TFREALESRGAN_REALESRGAN_H