package com.zhenxiang.superimage.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

val Context.writeStoragePermission: Boolean
    get() = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

val Context.appNeverUpdated: Boolean
    get() = with(packageManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            getPackageInfo(packageName, 0)
        }.let {
            it.lastUpdateTime == it.firstInstallTime
        }
    }
