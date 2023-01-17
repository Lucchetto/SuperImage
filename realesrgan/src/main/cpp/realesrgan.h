//
// Created by Zhenxiang Chen on 04/01/23.
//

#ifndef TFREALESRGAN_REALESRGAN_H
#define TFREALESRGAN_REALESRGAN_H

#include <android/log.h>

#include "Eigen/Core"

#define REALESRGAN_IMAGE_CHANNELS 3
#define REALESRGAN_INPUT_TILE_SIZE 64
#define REALESRGAN_INPUT_TILE_PADDING 10

#define LOG_TAG "RealESRGAN"
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))


const Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* run_inference(const void* model_data,
                                    const long model_size,
                                    int scale,
                                    const Eigen::MatrixXi& input_image);

#endif //TFREALESRGAN_REALESRGAN_H
