//
// Created by Zhenxiang Chen on 06/02/23.
//

#include "mnn_model.h"

mnn_model mnn_model_from_jbytes(JNIEnv* env, jbyteArray& jarray) {
    return {
        .data = env->GetByteArrayElements(jarray, nullptr),
        .size = env->GetArrayLength(jarray)
    };
}
