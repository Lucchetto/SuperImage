//
// Created by Zhenxiang Chen on 08/02/23.
//

#include "coroutine_utils.h"

extern "C" JNIEXPORT bool JNICALL
is_coroutine_scope_active(JNIEnv* env, jobject coroutine_scope) {
    jclass coroutine_scope_kt_clazz = env->FindClass("kotlinx/coroutines/CoroutineScopeKt");
    jmethodID method = env->GetStaticMethodID(
            coroutine_scope_kt_clazz,
            "isActive",
            "(Lkotlinx/coroutines/CoroutineScope;)Z");
    return env->CallStaticBooleanMethod(coroutine_scope_kt_clazz, method, coroutine_scope);
}
