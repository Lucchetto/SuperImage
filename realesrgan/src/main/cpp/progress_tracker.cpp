//
// Created by Zhenxiang Chen on 22/12/22.
//

#include "progress_tracker.h"

extern "C" JNIEXPORT void JNICALL
set_progress_percentage(JNIEnv* env, jobject progress_tracker, float percentage, long estimated_time) {
    jclass clazz = env->GetObjectClass(progress_tracker);
    jmethodID method = env->GetMethodID(clazz, "setProgress", "(FJ)V");
    env->CallVoidMethod(progress_tracker, method, percentage, estimated_time);
}
