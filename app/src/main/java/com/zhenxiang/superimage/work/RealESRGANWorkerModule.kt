package com.zhenxiang.superimage.work

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val RealESRGANWorkerModule = module {
    singleOf(::RealESRGANWorkerManager)
}
