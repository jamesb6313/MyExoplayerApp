package com.example.myexoplayerapp.util

import android.app.Activity
import android.os.Environment
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

class LogDump {


    companion object {
        //see http://android-delight.blogspot.com/2016/06/how-to-write-app-logcat-to-sd-card-in.html
        private val isExternalStorageReadOnly: Boolean get() {
            val extStorageState = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
        }
        private val isExternalStorageAvailable: Boolean get() {
            val extStorageState = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == extStorageState
        }

        fun writeLogCat(mActivity: Activity) {
            if (!isExternalStorageAvailable || isExternalStorageReadOnly) {
                throw Exception("Cannot access External Storage (none available or Read Only")
            }

            try {
                val process: Process = Runtime.getRuntime().exec("logcat -d -s myInfo:V")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                var log = "My Dump of Logcat\n"

                bufferedReader.use {
                    it.forEachLine { ln ->
                        log += ln + "\n"

                    }
                }

                val filepath = "MyFileStorage"
                val fileName = "logcat.txt"
                val sdCardFile = File(mActivity.getExternalFilesDir(filepath), fileName)

                Log.i("Dump", "writeLogCat sdCardFile = $sdCardFile")
                if (!sdCardFile.exists()) sdCardFile.createNewFile()


                val fileOutPutStream = FileOutputStream(sdCardFile)
                fileOutPutStream.write(log.toByteArray())
                fileOutPutStream.close()

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Log.e("Dump", "writeLogCat FileNotFound Error = ${e.message}")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.i("Dump", "writeLogCat I/O Error = ${e.message}")
            }


        }
    }
}