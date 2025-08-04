package com.meeting.accesscontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.meeting.accesscontrol.auto_launch.OptimizedBootManager

class OptimizedBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OptimizedBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        Log.d(TAG, "收到广播: $action")

        when (action) {
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.intent.action.PACKAGE_REPLACED",
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "com.your.package.DELAY_STARTUP" -> {
                handleBootEvent(context, action)
            }
        }
    }

    private fun handleBootEvent(context: Context, action: String) {
        try {
            Log.d(TAG, "处理开机事件: $action")

            // 检查是否应该处理
            if (!shouldHandleBootEvent(context, action)) {
                Log.d(TAG, "跳过处理")
                return
            }

            // 记录启动时间，防止重复启动
            val sharedPrefs = context.getSharedPreferences("boot_prefs", Context.MODE_PRIVATE)
            val lastBootTime = sharedPrefs.getLong("last_boot_time", 0L)
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastBootTime < 30000) {
                Log.d(TAG, "检测到重复启动，跳过")
                return
            }

            sharedPrefs.edit().putLong("last_boot_time", currentTime).apply()

            // 调度多策略启动
            OptimizedBootManager.scheduleMultiStrategyStartup(context)

        } catch (e: Exception) {
            Log.e(TAG, "处理开机事件失败", e)
        }
    }

    private fun shouldHandleBootEvent(context: Context, action: String): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.applicationInfo!!.enabled
        } catch (e: Exception) {
            Log.e(TAG, "检查应用状态失败", e)
            false
        }
    }
}