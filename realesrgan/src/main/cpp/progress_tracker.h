//
// Created by Zhenxiang Chen on 22/12/22.
//

#ifndef REALESRGAN_PROGRESS_TRACKER_H
#define REALESRGAN_PROGRESS_TRACKER_H

#include <jni.h>

extern "C" JNIEXPORT void JNICALL
set_progress_indeterminate(JNIEnv* env, jobject progress_tracker);

extern "C" JNIEXPORT void JNICALL
set_progress_percentage(JNIEnv* env, jobject progress_tracker, float percentage);

extern "C" JNIEXPORT void JNICALL
set_progress_success(JNIEnv* env, jobject progress_tracker);

extern "C" JNIEXPORT void JNICALL
set_progress_error(JNIEnv* env, jobject progress_tracker);

#endif //REALESRGAN_PROGRESS_TRACKER_H
