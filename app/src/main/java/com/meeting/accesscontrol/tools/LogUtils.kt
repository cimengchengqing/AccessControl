package com.meeting.accesscontrol.tools

import android.util.Log
import com.meeting.accesscontrol.net.BuildConfig

object LogUtils {
    // 日志开关，默认根据BuildConfig决定
    private val isDebug = BuildConfig.DEBUG

    // 输出Debug日志（仅Debug模式生效）
    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }

    // 输出Info日志（始终生效）
    fun i(tag: String, message: String) {
        if (isDebug) {
            Log.i(tag, message)
        }
    }

    // 输出Error日志（始终生效）
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            Log.e(tag, message)
        }
    }

    // 输出Warn日志（始终生效）
    fun w(tag: String, message: String) {
        if (isDebug) {
            Log.w(tag, message)
        }
    }
}
