//
// Created by Zhenxiang Chen on 04/01/23.
//

#ifndef TFREALESRGAN_REALESRGAN_H
#define TFREALESRGAN_REALESRGAN_H

#include <android/log.h>

#include "Eigen/Eigen"

#define REALESRGAN_IMAGE_CHANNELS 3
#define REALESRGAN_INPUT_TILE_SIZE 64
#define REALESRGAN_INPUT_TILE_PADDING 10

#define LOG_TAG "RealESRGAN"
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

/**
 * @param data the data stored in the array
 * @param size the size of the array in floats
 */
struct float_ptr_array {
    float* data;
    size_t size;
};

struct output_image_t {
    int* data;
    const size_t size;
};

const output_image_t* run_inference(const void* model_data,
                                    const long model_size,
                                    int scale,
                                    const Eigen::MatrixXi& input_image);

#endif //TFREALESRGAN_REALESRGAN_H
