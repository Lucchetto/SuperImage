#include <jni.h>
#include <string>

#include "mnn_model.h"
#include "upscaling.h"

extern "C" JNIEXPORT jobject JNICALL
Java_com_zhenxiang_realesrgan_RealESRGAN_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jobject progress_tracker,
        jobject coroutine_scope,
        jbyteArray model_data_jarray,
        jint scale,
        jintArray input_image_jarray,
        jint input_image_width,
        jint input_image_height) {

    const mnn_model model = mnn_model_from_jbytes(env, model_data_jarray);

    Eigen::Map<Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>> input_image(
            env->GetIntArrayElements(input_image_jarray, nullptr),
            input_image_height,
            input_image_width);

    const auto output_image = run_inference(env, progress_tracker, coroutine_scope, &model, scale, input_image);

    // Cleanup and return data
    env->ReleaseByteArrayElements(model_data_jarray, model.data, JNI_OK);
    env->ReleaseIntArrayElements(input_image_jarray, input_image.data(), JNI_OK);
    return env->NewDirectByteBuffer((void *) output_image.data, output_image.size * sizeof(int));
}
extern "C"
JNIEXPORT void JNICALL
Java_com_zhenxiang_realesrgan_RealESRGAN_freeDirectBuffer(
        JNIEnv *env,
        jobject /* thiz */,
        jobject buffer) {
    free(env->GetDirectBufferAddress(buffer));
}