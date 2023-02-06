//
// Created by Zhenxiang Chen on 06/02/23.
//

#ifndef SUPERIMAGE_LOGGING_H
#define SUPERIMAGE_LOGGING_H

#define LOG_TAG "RealESRGAN"
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

#endif //SUPERIMAGE_LOGGING_H
