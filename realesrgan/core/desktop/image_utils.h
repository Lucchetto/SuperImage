//
// Created by Zhenxiang Chen on 26/02/23.
//

#ifndef SUPERIMAGE_IMAGE_UTILS_H
#define SUPERIMAGE_IMAGE_UTILS_H

#include <jni.h>
#include "../image_dimensions.h"

extern "C" JNIEXPORT image_dimensions JNICALL
get_image_dimensions(JNIEnv* env, jobject buffered_image);

extern "C" JNIEXPORT jintArray JNICALL
get_rgb(JNIEnv* env, jobject buffered_image, image_dimensions dimens);

#endif //SUPERIMAGE_IMAGE_UTILS_H
