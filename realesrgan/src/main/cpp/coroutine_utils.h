//
// Created by Zhenxiang Chen on 08/02/23.
//

#ifndef SUPERIMAGE_COROUTINE_UTILS_H
#define SUPERIMAGE_COROUTINE_UTILS_H

#include <jni.h>

extern "C" JNIEXPORT bool JNICALL
is_coroutine_scope_active(JNIEnv* env, jobject coroutine_scope);

#endif //SUPERIMAGE_COROUTINE_UTILS_H
