package com.zhenxiang.superimage.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

val Context.writeStoragePermission: Boolean
    get() = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
