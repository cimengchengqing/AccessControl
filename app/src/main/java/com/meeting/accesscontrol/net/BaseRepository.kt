package com.meeting.accesscontrol.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 基础仓库类 - 封装网络请求
 */
abstract class BaseRepository {

    /**
     * 安全的网络请求执行
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): NetworkResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.success) {
                            NetworkResult.Success(body.data!!)
                        } else {
                            NetworkResult.Error(body.code, body.message)
                        }
                    } else {
                        NetworkResult.Error(NetworkConfig.ERROR_CODE, "响应体为空")
                    }
                } else {
                    NetworkResult.Error(response.code(), getHttpErrorMessage(response.code()))
                }
            } catch (e: Exception) {
                Log.e("BaseRepository", "网络请求异常", e)
                NetworkResult.Error(NetworkConfig.ERROR_CODE, getExceptionMessage(e), e)
            }
        }
    }

    /**
     * 无数据响应的网络请求
     */
    protected suspend fun safeApiCallNoData(
        apiCall: suspend () -> Response<ApiResponse<Any>>
    ): NetworkResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.success) {
                            NetworkResult.Success(Unit)
                        } else {
                            NetworkResult.Error(body.code, body.message)
                        }
                    } else {
                        NetworkResult.Error(NetworkConfig.ERROR_CODE, "响应体为空")
                    }
                } else {
                    NetworkResult.Error(response.code(), getHttpErrorMessage(response.code()))
                }
            } catch (e: Exception) {
                Log.e("BaseRepository", "网络请求异常", e)
                NetworkResult.Error(NetworkConfig.ERROR_CODE, getExceptionMessage(e), e)
            }
        }
    }

    /**
     * 获取HTTP错误信息
     */
    private fun getHttpErrorMessage(code: Int): String {
        return when (code) {
            NetworkConfig.HTTP_UNAUTHORIZED -> "认证失败，请重新登录"
            NetworkConfig.HTTP_FORBIDDEN -> "没有访问权限"
            NetworkConfig.HTTP_NOT_FOUND -> "请求的资源不存在"
            NetworkConfig.HTTP_INTERNAL_ERROR -> "服务器内部错误"
            in 500..599 -> "服务器错误"
            in 400..499 -> "请求错误"
            else -> "网络请求失败($code)"
        }
    }

    /**
     * 获取异常错误信息
     */
    private fun getExceptionMessage(exception: Exception): String {
        return when (exception) {
            is UnknownHostException -> "网络连接失败，请检查网络设置"
            is ConnectException -> "连接服务器失败"
            is SocketTimeoutException -> "网络请求超时"
            else -> exception.message ?: "未知错误"
        }
    }
}