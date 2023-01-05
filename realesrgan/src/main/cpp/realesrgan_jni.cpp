#include <jni.h>
#include <string>

#include "realesrgan.h"

extern "C" JNIEXPORT jintArray JNICALL
Java_com_zhenxiang_realesrgan_RealESRGAN_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jobject model_buffer,
        jint scale,
        jintArray input_image_jarray,
        jint input_image_width,
        jint input_image_height) {

    const void *model_data = static_cast<void *>(env->GetDirectBufferAddress(model_buffer));
    const jlong model_size_bytes = env->GetDirectBufferCapacity(model_buffer);

    Eigen::TensorMap<Eigen::Tensor<int, 2>> input_image(
            env->GetIntArrayElements(input_image_jarray, nullptr),
            input_image_height,
            input_image_width);

    const auto output_image = run_inference(model_data, model_size_bytes, scale, input_image);

    // Cleanup and return data
    env->ReleaseIntArrayElements(input_image_jarray, input_image.data(), JNI_OK);
    if (!output_image) {
        return nullptr;
    } else {
        jintArray output_image_jarray = env->NewIntArray(output_image->size);
        env->SetIntArrayRegion(output_image_jarray, 0, output_image->size, output_image->data);

        free(output_image->data);
        delete output_image;
        return output_image_jarray;
    }
}