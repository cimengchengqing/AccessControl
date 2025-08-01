package com.meeting.accesscontrol.tools

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppSPUtils private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "token_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        @Volatile
        private var instance: AppSPUtils? = null

        fun getInstance(context: Context): AppSPUtils =
            instance ?: synchronized(this) {
                instance ?: AppSPUtils(context.applicationContext).also {
                    instance = it
                }
            }
    }

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit { putString("access_token", value) }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit { putString("refresh_token", value) }

    var expiresAt: Long
        get() = prefs.getLong("expires_at", 0L)
        set(value) = prefs.edit { putLong("expires_at", value) }

    var refreshExpiresAt: Long
        get() = prefs.getLong("refresh_expires_at", 0L)
        set(value) = prefs.edit { putLong("refresh_expires_at", value) }

    fun isTokenExpired(currentTime: Long = System.currentTimeMillis()): Boolean =
        expiresAt <= currentTime

    fun isRefreshTokenExpired(currentTime: Long = System.currentTimeMillis()): Boolean =
        refreshExpiresAt <= currentTime

    fun clearTokens() = prefs.edit {
        remove("access_token")
        remove("refresh_token")
        remove("expires_at")
    }

    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresIn: Int = 7200
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiresAt = System.currentTimeMillis() + expiresIn * 1000L
        this.refreshExpiresAt = System.currentTimeMillis() + 28800 * 1000L  //默认8个小时
    }

    fun saveTokens(
        accessToken: String,
        expiresIn: Int = 7200
    ) {
        this.accessToken = accessToken
        this.expiresAt = System.currentTimeMillis() + expiresIn * 1000L
    }
}
