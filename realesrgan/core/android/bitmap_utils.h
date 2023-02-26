//
// Created by Zhenxiang Chen on 18/02/23.
//

#ifndef SUPERIMAGE_BITMAP_UTILS_H
#define SUPERIMAGE_BITMAP_UTILS_H

#include <jni.h>

struct bitmap_dimensions {
    int width;
    int height;
};

extern "C" JNIEXPORT bitmap_dimensions JNICALL
get_bitmap_dimensions(JNIEnv* env, jobject bitmap);

#endif //SUPERIMAGE_BITMAP_UTILS_H
