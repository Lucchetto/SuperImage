//
// Created by Zhenxiang Chen on 26/02/23.
//

#include <jni.h>

#include "image_utils.h"
#include "../image_tile_interpreter.h"
#include "../jni_common/mnn_model.h"
#include "../upscaling.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_zhenxiang_realesrgan_RealESRGAN_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jobject progress_tracker,
        jobject coroutine_scope,
        jbyteArray model_data_jarray,
        jint scale,
        jobject input_image,
        jintArray output_image_array) {

    const mnn_model model = mnn_model_from_jbytes(env, model_data_jarray);

    const image_dimensions input_dimens = get_image_dimensions(env, input_image);
    const jintArray input_image_array = get_rgb(env, input_image, input_dimens);

    PixelMatrix input_image_matrix(
            reinterpret_cast<int32_t*>(env->GetIntArrayElements(input_image_array, nullptr)),
            input_dimens.height,
            input_dimens.width);

    PixelMatrix output_image_matrix(
            reinterpret_cast<int32_t*>(env->GetIntArrayElements(output_image_array, nullptr)),
            input_image_matrix.rows() * scale,
            input_image_matrix.cols() * scale);

    int result = 0;

    try {
        run_inference(env, progress_tracker, coroutine_scope, &model, scale, input_image_matrix, output_image_matrix);
    } catch (ImageTileInterpreterException& e) {
        result = e.error;
    }

    // Cleanup and return data
    env->ReleaseByteArrayElements(model_data_jarray, model.data, JNI_OK);
    env->ReleaseIntArrayElements(input_image_array, reinterpret_cast<jint*>(input_image_matrix.data()), JNI_COMMIT);
    env->ReleaseIntArrayElements(output_image_array, reinterpret_cast<jint*>(output_image_matrix.data()), JNI_COMMIT);

    return result;
}
