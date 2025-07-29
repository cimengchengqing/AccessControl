package com.meeting.accesscontrol.net

/**
 * 通用API响应数据模型
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
    val success: Boolean = code == 200
)

/**
 * 网络请求结果封装
 */
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val code: Int, val message: String, val exception: Throwable? = null) : NetworkResult<T>()
    data class Loading<T>(val isLoading: Boolean = true) : NetworkResult<T>()
}

/**
 * 分页响应数据模型
 */
data class PageResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)