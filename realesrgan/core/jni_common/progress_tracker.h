//
// Created by Zhenxiang Chen on 22/12/22.
//

#ifndef REALESRGAN_PROGRESS_TRACKER_H
#define REALESRGAN_PROGRESS_TRACKER_H

#include <cstdint>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL
set_progress_percentage(JNIEnv* env, jobject progress_tracker, float percentage, int64_t estimated_time = -1);

#endif //REALESRGAN_PROGRESS_TRACKER_H
