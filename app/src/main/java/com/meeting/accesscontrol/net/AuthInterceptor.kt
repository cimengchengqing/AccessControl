package com.meeting.accesscontrol.net

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 认证拦截器 - 自动添加Token
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 获取保存的Token
        val token = getToken()

        val newRequest = if (token.isNotEmpty()) {
            originalRequest.newBuilder()
                .addHeader(NetworkConfig.HEADER_AUTHORIZATION, "Bearer $token")
                .addHeader(NetworkConfig.HEADER_CONTENT_TYPE, NetworkConfig.CONTENT_TYPE_JSON)
                .addHeader(NetworkConfig.HEADER_USER_AGENT, getUserAgent())
                .build()
        } else {
            originalRequest.newBuilder()
                .addHeader(NetworkConfig.HEADER_CONTENT_TYPE, NetworkConfig.CONTENT_TYPE_JSON)
                .addHeader(NetworkConfig.HEADER_USER_AGENT, getUserAgent())
                .build()
        }

        return chain.proceed(newRequest)
    }

    private fun getToken(): String {
        return sharedPreferences.getString("access_token", "") ?: ""
    }

    private fun getUserAgent(): String {
        return "SalesCall-Android/1.0"
    }

    companion object {
        fun saveToken(context: Context, token: String) {
            context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
                .edit()
                .putString("access_token", token)
                .apply()
        }

        fun clearToken(context: Context) {
            context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
                .edit()
                .remove("access_token")
                .apply()
        }
    }
}