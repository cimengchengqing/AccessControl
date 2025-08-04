package com.meeting.accesscontrol.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.meeting.accesscontrol.auto_launch.OptimizedBootManager

/**
 * 前台服务
 */
class BootStartupService : android.app.Service() {

    companion object {
        private const val TAG = "BootStartupService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "boot_startup_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "前台服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "前台服务启动")

        // 创建前台通知
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 尝试启动应用
        OptimizedBootManager.tryStartApp(this)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "前台服务销毁")

        // 服务被销毁时，尝试重启
        restartService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "开机自启动服务",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持应用运行的服务"
                setShowBadge(false)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val builder = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("应用运行中")
            .setContentText("应用正在后台运行")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)

        return builder.build()
    }

    private fun restartService() {
        try {
            Log.d(TAG, "尝试重启服务")
            val intent = Intent(this, BootStartupService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "重启服务失败", e)
        }
    }
}