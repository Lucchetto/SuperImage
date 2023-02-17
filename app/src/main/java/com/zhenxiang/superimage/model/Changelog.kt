package com.zhenxiang.superimage.model

sealed class Changelog {

    object Loading : Changelog()

    data class Show(val items: List<String>) : Changelog()

    object Hide : Changelog()
}
