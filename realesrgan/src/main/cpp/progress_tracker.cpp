//
// Created by Zhenxiang Chen on 22/12/22.
//

#include "progress_tracker.h"

extern "C" JNIEXPORT void JNICALL
set_progress_indeterminate(JNIEnv* env, jobject progress_tracker) {
    jclass clazz = env->GetObjectClass(progress_tracker);
    jmethodID method = env->GetMethodID(clazz, "setIndeterminate", "()V");
    env->CallVoidMethod(progress_tracker, method);
}

extern "C" JNIEXPORT void JNICALL
set_progress_percentage(JNIEnv* env, jobject progress_tracker, float percentage) {
    jclass clazz = env->GetObjectClass(progress_tracker);
    jmethodID method = env->GetMethodID(clazz, "setLoadingPercentage", "(F)V");
    env->CallVoidMethod(progress_tracker, method, percentage);
}

extern "C" JNIEXPORT void JNICALL
set_progress_success(JNIEnv* env, jobject progress_tracker) {
    jclass clazz = env->GetObjectClass(progress_tracker);
    jmethodID method = env->GetMethodID(clazz, "setSuccess", "()V");
    env->CallVoidMethod(progress_tracker, method);
}

extern "C" JNIEXPORT void JNICALL
set_progress_error(JNIEnv* env, jobject progress_tracker) {
    jclass clazz = env->GetObjectClass(progress_tracker);
    jmethodID method = env->GetMethodID(clazz, "setError", "()V");
    env->CallVoidMethod(progress_tracker, method);
}
