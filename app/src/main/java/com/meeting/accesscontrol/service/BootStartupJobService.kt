package com.meeting.accesscontrol.service

import android.util.Log
import com.meeting.accesscontrol.auto_launch.OptimizedBootManager

/**
 * JobScheduler服务
 */
class BootStartupJobService : android.app.job.JobService() {

    companion object {
        private const val TAG = "BootStartupJobService"
    }

    override fun onStartJob(params: android.app.job.JobParameters?): Boolean {
        Log.d(TAG, "JobScheduler任务开始")

        try {
            // 尝试启动应用
            OptimizedBootManager.tryStartApp(this)

            // 任务完成
            jobFinished(params, false)

        } catch (e: Exception) {
            Log.e(TAG, "JobScheduler任务失败", e)
            jobFinished(params, true) // 需要重试
        }

        return true
    }

    override fun onStopJob(params: android.app.job.JobParameters?): Boolean {
        Log.d(TAG, "JobScheduler任务停止")
        return false
    }
}