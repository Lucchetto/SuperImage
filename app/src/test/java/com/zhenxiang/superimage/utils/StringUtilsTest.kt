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

    @Test
    fun testAddFileNameSuffix() {
        val fileName1 = "file"
        assertEquals("file_suffix", fileName1.addFileNameSuffix("_suffix"))

        val fileName2 = "file.txt"
        assertEquals("file_suffix.txt", fileName2.addFileNameSuffix("_suffix"))

        val fileName3 = "..file.txt"
        assertEquals("..file_suffix.txt", fileName3.addFileNameSuffix("_suffix"))
    }
}