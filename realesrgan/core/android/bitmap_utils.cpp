//
// Created by Zhenxiang Chen on 18/02/23.
//

#include "bitmap_utils.h"

extern "C" JNIEXPORT bitmap_dimensions JNICALL
get_bitmap_dimensions(JNIEnv* env, jobject bitmap) {
    jclass clazz = env->GetObjectClass(bitmap);
    return {
        .width = env->CallIntMethod(bitmap, env->GetMethodID(clazz, "getWidth", "()I")),
        .height = env->CallIntMethod(bitmap, env->GetMethodID(clazz, "getHeight", "()I"))
    };
}

