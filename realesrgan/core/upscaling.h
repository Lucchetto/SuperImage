//
// Created by Zhenxiang Chen on 04/01/23.
//

#ifndef SUPERIMAGE_UPSCALING_H
#define SUPERIMAGE_UPSCALING_H

#include "jni.h"

#include "Eigen/Core"

#include "jni_common/mnn_model.h"

#define REALESRGAN_INPUT_TILE_SIZE 84
#define REALESRGAN_INPUT_TILE_PADDING 10

void run_inference(
        JNIEnv* jni_env,
        jobject progress_tracker,
        jobject coroutine_scope,
        const mnn_model* model,
        int scale,
        const Eigen::MatrixXi& input_image_matrix,
        Eigen::Map<Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>& output_image_matrix);

#endif //SUPERIMAGE_UPSCALING_H
