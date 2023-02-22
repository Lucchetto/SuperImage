package com.zhenxiang.superimage.shared.model

sealed class Changelog {

    object Loading : Changelog()

    data class Show(val items: List<String>) : Changelog()

    object Hide : Changelog()
}
