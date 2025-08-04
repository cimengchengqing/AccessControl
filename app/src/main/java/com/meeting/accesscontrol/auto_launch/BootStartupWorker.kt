package com.meeting.accesscontrol.auto_launch

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * WorkManager工作器
 */
class BootStartupWorker (context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val TAG = "BootStartupWorker"
    }

    override fun doWork(): Result {
        Log.d(TAG, "WorkManager任务开始")

        return try {
            // 尝试启动应用
            OptimizedBootManager.tryStartApp(applicationContext)

            Log.d(TAG, "WorkManager任务完成")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "WorkManager任务失败", e)
            Result.retry()
        }
    }
}