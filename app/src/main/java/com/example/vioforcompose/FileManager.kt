package com.example.vioforcompose.ui.theme

import android.icu.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date

class FileManager(private val foldername: String, toInputDatabase: () -> Unit = {}) {
    var rawfilename: String = "temp.txt"
    var filename: String = "temp.csv"

    fun setFileName(rawFileName:String, fileName:String){
        this.rawfilename = rawFileName; this.filename = fileName
    }
    fun setFileTime(){
        rawfilename = "rawSerial_" + SimpleDateFormat("yyyy_MM_dd HH_mm_ss").format(Date()) + ".txt"
        filename = SimpleDateFormat("yyyy_MM_dd HH_mm_ss").format(Date()) + ".csv"
    }

    suspend fun writeTextFile(contents: String) {
        withContext(Dispatchers.IO) {
            try {
                val dir = File(foldername)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                val fos = FileOutputStream(foldername + "/" + rawfilename, true)
                fos.write(contents.encodeToByteArray())
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


}