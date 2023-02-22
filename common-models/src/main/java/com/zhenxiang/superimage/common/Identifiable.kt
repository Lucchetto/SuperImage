package com.zhenxiang.superimage.common

interface Identifiable<T: Comparable<T>> {

    val id: T

    interface EnumCompanion<T> where T : Enum<T>, T: Identifiable<*> {

        val VALUES: Array<T>

        fun fromId(id: Int?) = id?.let { VALUES.firstOrNull { item -> item.id == it } }
    }
}
