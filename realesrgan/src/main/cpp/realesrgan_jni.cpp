#include <jni.h>
#include <string>

#include "realesrgan.h"

extern "C" JNIEXPORT jintArray JNICALL
Java_com_zhenxiang_realesrgan_RealESRGAN_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jobject progress_tracker,
        jbyteArray model_data_jarray,
        jint scale,
        jintArray input_image_jarray,
        jint input_image_width,
        jint input_image_height) {

    jbyte* model_data = env->GetByteArrayElements(model_data_jarray, nullptr);
    const jlong model_size_bytes = env->GetArrayLength(model_data_jarray);

    Eigen::Map<Eigen::Matrix<int, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>> input_image(
            env->GetIntArrayElements(input_image_jarray, nullptr),
            input_image_height,
            input_image_width);

    const auto output_image = run_inference(env, progress_tracker, model_data, model_size_bytes, scale, input_image);

    // Cleanup and return data
    env->ReleaseByteArrayElements(model_data_jarray, model_data, JNI_OK);
    env->ReleaseIntArrayElements(input_image_jarray, input_image.data(), JNI_OK);
    if (!output_image) {
        return nullptr;
    } else {
        jintArray output_image_jarray = env->NewIntArray(output_image->size());
        env->SetIntArrayRegion(output_image_jarray, 0, output_image->size(), output_image->data());

        delete output_image;
        return output_image_jarray;
    }
}