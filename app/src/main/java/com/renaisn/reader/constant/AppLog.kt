package com.renaisn.reader.constant

import android.util.Log
import com.renaisn.reader.BuildConfig
import com.renaisn.reader.help.config.AppConfig

object AppLog {

    private val mLogs = arrayListOf<Triple<Long, String, Throwable?>>()

    val logs get() = mLogs.toList()

    @Synchronized
    fun put(message: String?, throwable: Throwable? = null) {
        message ?: return
        if (mLogs.size > 100) {
            mLogs.removeLastOrNull()
        }
        mLogs.add(0, Triple(System.currentTimeMillis(), message, throwable))
        if (throwable != null) {
            if (com.renaisn.reader.BuildConfig.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace
                Log.e(stackTrace[3].className, message, throwable)
            }
        }
    }

    @Synchronized
    fun clear() {
        mLogs.clear()
    }

    fun putDebug(message: String?, throwable: Throwable? = null) {
        if (AppConfig.recordLog || com.renaisn.reader.BuildConfig.DEBUG) {
            put(message, throwable)
        }
    }

}