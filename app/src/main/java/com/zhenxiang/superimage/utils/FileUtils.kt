package com.zhenxiang.superimage.utils

import java.io.File

object FileUtils {

    private val FILE_EXTENSION_CHAR = '.'

    /**
     * Create file object with given name, if file with given name already exists append
     * a numerical suffix. Only file object will be created, make sure to call [File.mkdir]
     * or [File.createNewFile] afterwards
     */
    fun newFileAutoSuffix(parentDir: File, fileName: String): File {
        File(parentDir, fileName).let {
            // Existing file with given name not found, success
            if (!it.exists()) {
                return it
            }
        }
        val fileNameNoExt: String
        val extension: String
        fileName.lastIndexOf(FILE_EXTENSION_CHAR).let {
            if (it > -1) {
                fileNameNoExt = fileName.substring(0, it)
                extension = fileName.substring(it, fileName.length)
            } else {
                fileNameNoExt = fileName
                extension = ""
            }
        }

        // Look for existing files with suffix until we find a free filename
        var suffixNumber = 1
        while (true) {
            val file = File(parentDir, "$fileNameNoExt ($suffixNumber)$extension")
            if (!file.exists()) {
                return file
            }
            suffixNumber++
        }
    }
}