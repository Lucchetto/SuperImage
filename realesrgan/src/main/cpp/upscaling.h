//
// Created by Zhenxiang Chen on 04/01/23.
//

#ifndef SUPERIMAGE_UPSCALING_H
#define SUPERIMAGE_UPSCALING_H

#include <android/log.h>
#include "jni.h"

#include "Eigen/Core"

#include "mnn_model.h"

#define REALESRGAN_IMAGE_CHANNELS 3
#define REALESRGAN_INPUT_TILE_SIZE 84
#define REALESRGAN_INPUT_TILE_PADDING 10

#define LOG_TAG "RealESRGAN"
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))


const Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>* run_inference(
        JNIEnv* jni_env,
        jobject progress_tracker,
        const mnn_model* model,
        int scale,
        const Eigen::MatrixXi& input_image);

#endif //SUPERIMAGE_UPSCALING_H
