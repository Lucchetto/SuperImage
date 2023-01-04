#include <jni.h>
#include <string>

#include "realesrgan.h"

extern "C" JNIEXPORT jobject JNICALL
Java_com_zhenxiang_realesrgan_RealESRGAN_runUpscaling(
        JNIEnv *env,
        jobject /* thiz */,
        jobject model_buffer,
        jint scale,
        jintArray input_image_jarray) {

    const void *model_data = static_cast<void *>(env->GetDirectBufferAddress(model_buffer));
    const jlong model_size_bytes = env->GetDirectBufferCapacity(model_buffer);

    jint *input_image = env->GetIntArrayElements(input_image_jarray, nullptr);

    const auto output_image = run_inference(model_data, model_size_bytes, scale, input_image);

    // Cleanup and return data
    env->ReleaseIntArrayElements(input_image_jarray, input_image, JNI_OK);
    if (!output_image) {
        return nullptr;
    } else {
        return env->NewDirectByteBuffer((void *) output_image->data, output_image->size);
    }
}