//
// Created by Zhenxiang Chen on 04/01/23.
//

#ifndef TFREALESRGAN_REALESRGAN_H
#define TFREALESRGAN_REALESRGAN_H

#include <android/log.h>

#include "Eigen/CXX11/Tensor"

#define REALESRGAN_IMAGE_CHANNELS 3

#define LOG_TAG "RealESRGAN"
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

struct output_image_t {
    int* data;
    const int size;
};

const output_image_t* run_inference(const void* model_data,
                                    const long model_size,
                                    int scale,
                                    const Eigen::TensorMap<Eigen::Tensor<int, 2>> input_image);

#endif //TFREALESRGAN_REALESRGAN_H
