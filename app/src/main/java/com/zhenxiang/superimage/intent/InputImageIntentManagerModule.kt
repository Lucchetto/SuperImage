package com.zhenxiang.superimage.intent

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val InputImageIntentManagerModule = module {
    singleOf(::InputImageIntentManager)
}
