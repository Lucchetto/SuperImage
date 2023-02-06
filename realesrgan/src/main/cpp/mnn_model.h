//
// Created by Zhenxiang Chen on 06/02/23.
//

#ifndef SUPERIMAGE_MNN_MODEL_H
#define SUPERIMAGE_MNN_MODEL_H

#include "jni.h"

struct mnn_model {
    int8_t* data;
    long size;
};

mnn_model mnn_model_from_jbytes(JNIEnv* env, jbyteArray& jarray);

#endif //SUPERIMAGE_MNN_MODEL_H
