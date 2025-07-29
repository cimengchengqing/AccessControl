package com.meeting.accesscontrol.net

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset

/**
 * 网络请求日志拦截器
 */
class LoggingInterceptor(private val isDebug: Boolean = true) : Interceptor {
    private val TAG = "NetworkRequest"
    private val UTF8 = Charset.forName("UTF-8")

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!isDebug) {
            return chain.proceed(request)
        }

        val startTime = System.currentTimeMillis()

        // 打印请求信息
        logRequest(request)

        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()

        // 打印响应信息
        logResponse(response, endTime - startTime)

        return response
    }

    private fun logRequest(request: okhttp3.Request) {
        try {
            Log.d(TAG, "╔══════════════════ REQUEST ══════════════════")
            Log.d(TAG, "║ URL: ${request.url}")
            Log.d(TAG, "║ Method: ${request.method}")

            // 打印请求头
            if (request.headers.size > 0) {
                Log.d(TAG, "║ Headers:")
                for (i in 0 until request.headers.size) {
                    Log.d(TAG, "║   ${request.headers.name(i)}: ${request.headers.value(i)}")
                }
            }

            // 打印请求体
            request.body?.let { requestBody ->
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                val charset = requestBody.contentType()?.charset(UTF8) ?: UTF8
                val content = buffer.readString(charset)
                if (content.isNotEmpty()) {
                    Log.d(TAG, "║ Body: $content")
                }
            }

            Log.d(TAG, "╚══════════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "打印请求日志失败: ${e.message}")
        }
    }

    private fun logResponse(response: Response, duration: Long) {
        try {
            Log.d(TAG, "╔══════════════════ RESPONSE ═════════════════")
            Log.d(TAG, "║ URL: ${response.request.url}")
            Log.d(TAG, "║ Code: ${response.code}")
            Log.d(TAG, "║ Message: ${response.message}")
            Log.d(TAG, "║ Duration: ${duration}ms")

            // 打印响应头
            if (response.headers.size > 0) {
                Log.d(TAG, "║ Headers:")
                for (i in 0 until response.headers.size) {
                    Log.d(TAG, "║   ${response.headers.name(i)}: ${response.headers.value(i)}")
                }
            }

            // 打印响应体
            val responseBody = response.body
            if (responseBody != null) {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer
                val charset = responseBody.contentType()?.charset(UTF8) ?: UTF8
                val content = buffer.clone().readString(charset)
                if (content.isNotEmpty()) {
                    Log.d(TAG, "║ Body: $content")
                }
            }

            Log.d(TAG, "╚══════════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "打印响应日志失败: ${e.message}")
        }
    }
}