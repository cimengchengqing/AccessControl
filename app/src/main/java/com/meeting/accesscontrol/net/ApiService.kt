package com.meeting.accesscontrol.net

import com.meeting.accesscontrol.bean.JudgeRequest
import com.meeting.accesscontrol.bean.MeetingRoom
import com.meeting.accesscontrol.tools.AppConfig.Companion.APP_ID
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * API接口服务
 */
interface ApiService {

    /**
     * 请求会议列表信息
     */
    @GET("openapi/meeting")
    suspend fun getMeetingsByID(
        @Header("nonce") nonce: String,
        @Header("timestamp") timestamp: String,
        @Header("appid") appid: String = APP_ID,
        @Header("sign") sign: String,
        @Query("deviceNumber") deviceNumber: String
    ): Response<AppResponse<MeetingRoom>>

    /**
     * 人脸识别
     */
    @Multipart
    @POST("http://39.105.116.125:9562/api/v1/recognition/recognize")
    suspend fun uploadFaceInfo(
        @Header("x-api-key") apiKey: String,
        @Part file: MultipartBody.Part        // 人脸图片
    ): Response<FaceResponse>

    /**
     * 门禁校验
     */
    @POST("openapi/judge")
    suspend fun accessControlJudge(
        @Header("nonce") nonce: String,
        @Header("timestamp") timestamp: String,
        @Header("appid") appid: String = APP_ID,
        @Header("sign") sign: String,
        @Body request: JudgeRequest
    ): Response<AppResponse<String>>


    /**
     * 设置门状态（门禁开关）
     * @param request 门状态请求参数
     * @return 响应结果
     */
    @PUT("http://172.40.10.20:11125/api/mg/v3/egs/control/door-status")
    suspend fun setDoorStatus(@Body request: DoorStatusRequest): Response<ResponseBody>

    /**
     * 请求响应结果
     */
    data class AppResponse<T>(
        val code: Int,
        val message: String,
        val data: T
    )

    /**
     * 门状态请求参数
     */
    data class DoorStatusRequest(
        val channel_nos: List<String>,
        val door_status: Int
    )

    /**
     * 人脸识别相应结果
     */
    data class FaceResponse(
        val result: List<FaceRResult>
    )

    data class FaceRResult(
        val subjects: List<UserFace>
    )

    data class UserFace(
        val subject: String,     // 用户ID
        val similarity: Double   // 相似度
    )
}