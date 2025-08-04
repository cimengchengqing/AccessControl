package com.meeting.accesscontrol.auto_launch

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.meeting.accesscontrol.receiver.OptimizedBootReceiver
import com.meeting.accesscontrol.service.BootStartupJobService
import com.meeting.accesscontrol.service.BootStartupService
import com.meeting.accesscontrol.ui.MainActivity
import java.util.concurrent.TimeUnit


/**
 * 开机自启动管理器
 * 针对Android 11+的限制进行优化
 */
class OptimizedBootManager {

    companion object {
        private const val TAG = "OptimizedBootManager"
        private const val DELAY_STARTUP_ACTION = "com.your.package.DELAY_STARTUP"
        private const val JOB_ID = 1001
        private const val WORK_NAME = "boot_startup_work"

        /**
         * 调度多种启动策略
         */
        fun scheduleMultiStrategyStartup(context: Context) {
            try {
                Log.d(TAG, "调度多策略启动")

                // 策略1: AlarmManager延迟启动
                scheduleAlarmManagerStartup(context)

                // 策略2: JobScheduler调度
                scheduleJobSchedulerStartup(context)

                // 策略3: WorkManager调度
                scheduleWorkManagerStartup(context)

                // 策略4: 前台服务保活
                startForegroundService(context)

            } catch (e: Exception) {
                Log.e(TAG, "调度多策略启动失败", e)
            }
        }

        /**
         * 策略1: AlarmManager延迟启动
         */
        private fun scheduleAlarmManagerStartup(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // 1. 检查权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        // 2. 引导用户授权
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        Log.w(TAG, "缺少SCHEDULE_EXACT_ALARM权限")
                        return
                    }
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, OptimizedBootReceiver::class.java).apply {
                        action = DELAY_STARTUP_ACTION
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 设置多个时间点的延迟启动
                val delays = longArrayOf(5000L, 15000L, 30000L) // 5秒、15秒、30秒

                delays.forEach { delay ->
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delay,
                        pendingIntent
                    )

                }

                Log.d(TAG, "AlarmManager启动策略已调度")

            } catch (e: Exception) {
                Log.e(TAG, "AlarmManager启动策略失败", e)
            }
        }

        /**
         * 策略2: JobScheduler调度
         */
        private fun scheduleJobSchedulerStartup(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val jobScheduler =
                        context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

                    val jobInfo = JobInfo.Builder(
                        JOB_ID,
                        ComponentName(context, BootStartupJobService::class.java)
                    )
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setMinimumLatency(10000L) // 10秒延迟
                        .setOverrideDeadline(60000L) // 60秒内执行
                        .setPersisted(true) // 持久化任务
                        .build()

                    val result = jobScheduler.schedule(jobInfo)
                    if (result == JobScheduler.RESULT_SUCCESS) {
                        Log.d(TAG, "JobScheduler启动策略已调度")
                    } else {
                        Log.e(TAG, "JobScheduler启动策略失败")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "JobScheduler启动策略失败", e)
            }
        }

        /**
         * 策略3: WorkManager调度
         */
        private fun scheduleWorkManagerStartup(context: Context) {
            try {
                val workRequest = OneTimeWorkRequestBuilder<BootStartupWorker>()
                    .setInitialDelay(8000L, TimeUnit.MILLISECONDS) // 8秒延迟
                    .build()

                WorkManager.getInstance(context)
                    .enqueue(workRequest)

                Log.d(TAG, "WorkManager启动策略已调度")

            } catch (e: Exception) {
                Log.e(TAG, "WorkManager启动策略失败", e)
            }
        }

        /**
         * 策略4: 前台服务保活
         */
        private fun startForegroundService(context: Context) {
            try {
                val intent = Intent(context, BootStartupService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }

                Log.d(TAG, "前台服务保活策略已启动")

            } catch (e: Exception) {
                Log.e(TAG, "前台服务保活策略失败", e)
            }
        }

        /**
         * 尝试启动应用（多种方式）
         */
        fun tryStartApp(context: Context) {
            try {
                Log.d(TAG, "尝试启动应用")

                // 检查启动条件
                if (!shouldStartApp(context)) {
                    Log.d(TAG, "不满足启动条件，跳过")
                    return
                }

                // 方式1: 直接启动Activity
                startAppViaActivity(context)

                // 方式2: 通过Intent启动
                startAppViaIntent(context)

                // 方式3: 通过PackageManager启动
                startAppViaPackageManager(context)

            } catch (e: Exception) {
                Log.e(TAG, "启动应用失败", e)
            }
        }

        /**
         * 方式1: 直接启动Activity
         */
        private fun startAppViaActivity(context: Context) {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
                Log.d(TAG, "通过Activity启动成功")
            } catch (e: Exception) {
                Log.e(TAG, "通过Activity启动失败", e)
            }
        }

        /**
         * 方式2: 通过Intent启动
         */
        private fun startAppViaIntent(context: Context) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d(TAG, "通过Intent启动成功")
            } catch (e: Exception) {
                Log.e(TAG, "通过Intent启动失败", e)
            }
        }

        /**
         * 方式3: 通过PackageManager启动
         */
        private fun startAppViaPackageManager(context: Context) {
            try {
                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Log.d(TAG, "通过PackageManager启动成功")
                }
            } catch (e: Exception) {
                Log.e(TAG, "通过PackageManager启动失败", e)
            }
        }

        /**
         * 检查是否应该启动应用
         */
        fun shouldStartApp(context: Context): Boolean {
            return try {
                // 检查是否已经启动过
                val sharedPrefs = context.getSharedPreferences("boot_prefs", Context.MODE_PRIVATE)
                val lastStartTime = sharedPrefs.getLong("last_start_time", 0L)
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastStartTime < 30000) {
                    Log.d(TAG, "距离上次启动时间过短，跳过")
                    return false
                }

                // 记录启动时间
                sharedPrefs.edit().putLong("last_start_time", currentTime).apply()

                true

            } catch (e: Exception) {
                Log.e(TAG, "检查启动条件失败", e)
                true // 出错时默认启动
            }
        }
    }
}