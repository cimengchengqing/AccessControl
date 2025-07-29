package com.meeting.accesscontrol.net

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 网络请求扩展函数
 */

/**
 * 将NetworkResult转换为Flow
 */
fun <T> NetworkResult<T>.asFlow(): Flow<NetworkResult<T>> = flow {
    emit(NetworkResult.Loading())
    emit(this@asFlow)
}

/**
 * 执行网络请求并返回Flow
 */
fun <T> flowApiCall(apiCall: suspend () -> NetworkResult<T>): Flow<NetworkResult<T>> = flow {
    emit(NetworkResult.Loading())
    try {
        val result = apiCall()
        emit(result)
    } catch (e: Exception) {
        emit(NetworkResult.Error(NetworkConfig.ERROR_CODE, e.message ?: "未知错误", e))
    }
}

/**
 * 处理NetworkResult的扩展函数
 */
inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (Int, String, Throwable?) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) {
        action(code, message, exception)
    }
    return this
}

inline fun <T> NetworkResult<T>.onLoading(action: (Boolean) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Loading) {
        action(isLoading)
    }
    return this
}

/**
 * 判断是否成功
 */
fun <T> NetworkResult<T>.isSuccess(): Boolean {
    return this is NetworkResult.Success
}

/**
 * 判断是否失败
 */
fun <T> NetworkResult<T>.isError(): Boolean {
    return this is NetworkResult.Error
}

/**
 * 判断是否加载中
 */
fun <T> NetworkResult<T>.isLoading(): Boolean {
    return this is NetworkResult.Loading
}

/**
 * 获取数据，失败时返回null
 */
fun <T> NetworkResult<T>.getDataOrNull(): T? {
    return if (this is NetworkResult.Success) data else null
}

/**
 * 获取错误信息
 */
fun <T> NetworkResult<T>.getErrorMessage(): String? {
    return if (this is NetworkResult.Error) message else null
}