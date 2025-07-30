package com.meeting.accesscontrol

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import com.meeting.accesscontrol.aotu_launch.BootStartupManager

class MyApp : Application(), CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()

        // 初始化开机启动管理器
        BootStartupManager.getInstance(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .setMinimumLoggingLevel(Log.ERROR)
            .build()
    }
}