package com.zhenxiang.superimage.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {

    @Test
    fun testReplaceFileExtension() {
        val fileName1 = "file"
        assertEquals("file.xxx", fileName1.replaceFileExtension("xxx"))

        val fileName2 = "file.txt"
        assertEquals("file.xxx", fileName2.replaceFileExtension("xxx"))

        val fileName3 = "file.old.txt"
        assertEquals("file.old.xxx", fileName3.replaceFileExtension("xxx"))
    }
}