#include <android/bitmap.h>
#include <jni.h>
#include <string>

#include "bitmap_utils.h"
#include "mnn_model.h"
#include "upscaling.h"

extern "C" JNIEXPORT void JNICALL
Java_com_zhenxiang_realesrgan_RealESRGAN_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jobject progress_tracker,
        jobject coroutine_scope,
        jbyteArray model_data_jarray,
        jint scale,
        jobject input_bitmap,
        jobject output_bitmap) {

    const mnn_model model = mnn_model_from_jbytes(env, model_data_jarray);

    void* input_bitmap_buffer;
    void* output_bitmap_buffer;
    AndroidBitmap_lockPixels(env, input_bitmap, &input_bitmap_buffer);
    AndroidBitmap_lockPixels(env, output_bitmap, &output_bitmap_buffer);

    const bitmap_dimensions input_dimens = get_bitmap_dimensions(env, input_bitmap);

    const Eigen::Map<Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>> input_image_matrix(
            (int*) input_bitmap_buffer,
            input_dimens.height,
            input_dimens.width);

    Eigen::Map<Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>> output_image_matrix(
            (int*) output_bitmap_buffer,
            input_image_matrix.rows() * scale,
            input_image_matrix.cols() * scale);

    run_inference(env, progress_tracker, coroutine_scope, &model, scale, input_image_matrix, output_image_matrix);

    // Cleanup and return data
    env->ReleaseByteArrayElements(model_data_jarray, model.data, JNI_OK);
    AndroidBitmap_unlockPixels(env, input_bitmap);
    AndroidBitmap_unlockPixels(env, output_bitmap);
}
