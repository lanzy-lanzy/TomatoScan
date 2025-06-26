package com.ml.tomatoscan.utils

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var context: Context? = null

    companion object {
        private var instance: CrashHandler? = null
        
        fun getInstance(): CrashHandler {
            if (instance == null) {
                instance = CrashHandler()
            }
            return instance!!
        }
    }

    fun init(context: Context) {
        this.context = context
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            // Log the crash details
            Log.e("CrashHandler", "Uncaught exception in thread ${thread.name}", exception)
            
            // Get stack trace as string
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            exception.printStackTrace(printWriter)
            val stackTrace = stringWriter.toString()
            
            Log.e("CrashHandler", "Stack trace: $stackTrace")
            
            // You can add crash reporting here (e.g., send to analytics)
            
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error in crash handler", e)
        } finally {
            // Call default handler to show system crash dialog
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}
