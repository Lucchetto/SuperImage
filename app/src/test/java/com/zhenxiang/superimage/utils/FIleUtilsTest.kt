package com.zhenxiang.superimage.utils

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


class FIleUtilsTest {

    @get:Rule
    val folderRule = TemporaryFolder()

    @Test
    fun testNewFileAutoSuffix(): Unit = with(FileUtils) {
        val parentDir = folderRule.newFolder("parent")
        // Create file for first time, no suffix should be added
        val file = newFileAutoSuffix(parentDir, "..file.txt").apply {
            createNewFile()
        }
        assertEquals("..file.txt", file.name)

        // The following files will have a suffix
        (1 until 10).forEach {
            val suffixedFile = newFileAutoSuffix(parentDir, "..file.txt").apply {
                createNewFile()
            }
            assertEquals("..file ($it).txt", suffixedFile.name)
        }
    }
}
