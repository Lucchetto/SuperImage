//
// Created by Zhenxiang Chen on 26/02/23.
//

#include "image_utils.h"

extern "C" JNIEXPORT image_dimensions JNICALL
get_image_dimensions(JNIEnv* env, jobject buffered_image) {
    jclass clazz = env->GetObjectClass(buffered_image);
    return {
        .width = env->CallIntMethod(buffered_image, env->GetMethodID(clazz, "getWidth", "()I")),
        .height = env->CallIntMethod(buffered_image, env->GetMethodID(clazz, "getHeight", "()I"))
    };
}

extern "C" JNIEXPORT jintArray JNICALL
get_rgb(JNIEnv* env, jobject buffered_image, image_dimensions dimens) {
    jclass clazz = env->GetObjectClass(buffered_image);
    return (jintArray) env->CallObjectMethod(
        clazz,
        env->GetMethodID(clazz, "getRGB", "(I, I, I, I, [I, I, I)[I"),
        0,
        0,
        dimens.width,
        dimens.height,
        nullptr,
        0,
        0
    );
}
