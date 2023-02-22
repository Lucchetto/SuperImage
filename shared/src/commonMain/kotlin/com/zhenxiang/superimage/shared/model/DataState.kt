package com.zhenxiang.superimage.shared.model

sealed class DataState<T, E> {

    data class Success<T, E>(val data: T): DataState<T, E>()

    class Loading<T, E> : DataState<T, E>()

    data class Error<T, E>(val error: E) : DataState<T, E>()
}