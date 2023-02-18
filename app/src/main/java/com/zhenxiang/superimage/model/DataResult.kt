package com.zhenxiang.superimage.model

sealed class DataResult<T, E> {

    data class Success<T, E>(val data: T): DataResult<T, E>()

    data class Error<T, E>(val error: E) : DataResult<T, E>()
}