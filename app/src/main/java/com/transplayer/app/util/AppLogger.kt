package com.transplayer.app.util

import android.util.Log
//import com.transplayer.app.BuildConfig

object AppLogger {
    private const val TAG = "TransPlayer"

    fun d(message: String, tag: String = TAG) {
//        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
//        }
    }

    fun i(message: String, tag: String = TAG) {
//        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
//        }
    }

    fun w(message: String, tag: String = TAG) {
        Log.w(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.e(tag, message, throwable)
    }
}





