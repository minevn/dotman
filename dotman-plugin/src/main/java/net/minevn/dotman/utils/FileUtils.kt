package net.minevn.dotman.utils

import java.io.File
import java.io.IOException

class FileUtils {
    companion object {
        @Throws(IOException::class)
        fun readLines(file: String): List<String> {
            return File(file).readLines(Charsets.UTF_8)
        }

        fun isFileExist(file: String): Boolean {
            return File(file).exists()
        }
    }
}