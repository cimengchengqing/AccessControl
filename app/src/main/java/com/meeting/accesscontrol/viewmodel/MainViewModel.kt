package com.meeting.accesscontrol.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meeting.accesscontrol.bean.JudgeRequest
import com.meeting.accesscontrol.bean.MeetingRoom
import com.meeting.accesscontrol.net.ApiService
import com.meeting.accesscontrol.tools.AppConfig
import com.meeting.accesscontrol.tools.CommonUtils
import com.meeting.accesscontrol.tools.ImageUploadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random

class MainViewModel(private val apiService: ApiService) : ViewModel() {

    private val _meetingResult = MutableLiveData<Result<MeetingRoom>>()
    val meetingResult: LiveData<Result<MeetingRoom>> = _meetingResult

    private val _uploadResult = MutableLiveData<Result<String>>()
    val uploadResult: LiveData<Result<String>> = _uploadResult

    //图片上传辅助工具
    private val imageUploadHelper = ImageUploadHelper()

    /**
     * 获取会议信息
     */
    fun requestMeetingInfo(deviceID: String) {
        val nonce = Random().nextInt(10000).toString()
        val timestamp = (System.currentTimeMillis()).toString()
        val sign = CommonUtils.md5(AppConfig.SECRET_KEY + timestamp + nonce)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    apiService.getMeetingsByID(nonce, timestamp, AppConfig.APP_ID, sign, deviceID)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        if (it.code == 0) {
                            _meetingResult.postValue(Result.success(body.data))
                        } else {
                            _meetingResult.postValue(Result.failure(Exception("请求出错: ${it.message}")))
                        }
                    }

                } else {
                    _meetingResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {

            }
        }
    }

    /**
     * 上传人脸识别信息
     */
    fun uploadFaceImage(bitmapFile: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {// 将Bitmap转换为MultipartBody.Part
                val filePart =
                    imageUploadHelper.bitmapToMultipartPart(bitmapFile, "file", "face_image.jpg")
                val response = apiService.uploadFaceInfo(AppConfig.X_API_KEY, filePart)
                if (response.code() == 200) {
                    if (response.body() != null) {
                        val result = response.body()?.result
                        result?.let {
                            val faceResult = it[0]
                            val faces = faceResult.subjects
                            val userId = faces.sortedByDescending { it.similarity }[0]
                            _uploadResult.postValue(Result.success("$userId"))
                        } ?: _uploadResult.postValue(Result.failure(Exception("请求出错")))
                    }
                    _uploadResult.postValue(Result.success("上传成功"))
                } else {
                    _uploadResult.postValue(Result.failure(Exception("请求出错")))
                }
            } catch (e: Exception) {

            }
        }
    }

    /**
     * 与会人员信息校验
     */
    fun verifyUserByMeeting(userID: Int, meetingID: Int) {
        val nonce = Random().nextInt(10000).toString()
        val timestamp = (System.currentTimeMillis()).toString()
        val sign = CommonUtils.md5(AppConfig.SECRET_KEY + timestamp + nonce)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = JudgeRequest(userID, meetingID)
                val response =
                    apiService.accessControlJudge(nonce, timestamp, AppConfig.APP_ID, sign, request)
                if (response.isSuccessful) {

                } else {

                }
            } catch (e: Exception) {

            }
        }
    }
}