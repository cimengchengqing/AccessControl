package com.meeting.accesscontrol.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meeting.accesscontrol.bean.JudgeRequest
import com.meeting.accesscontrol.bean.MeetingRoom
import com.meeting.accesscontrol.net.ApiService
import com.meeting.accesscontrol.net.ApiService.DoorStatusRequest
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

    private val _recognitionResult = MutableLiveData<Result<String>>()
    val recognitionResult: LiveData<Result<String>> = _recognitionResult

    private val _doorResult = MutableLiveData<Result<String>>()
    val doorResult: LiveData<Result<String>> = _doorResult

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
                    } ?: _meetingResult.postValue(Result.failure(Exception("请求出错")))
                } else {
                    _meetingResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {
                Log.e("主页", "requestMeetingInfo: ${e.message}")
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
                            val userId = faces.sortedByDescending { it.similarity }[0].subject
                            _uploadResult.postValue(Result.success("$userId"))
                        } ?: _uploadResult.postValue(Result.failure(Exception("请求出错")))
                    }
                } else {
                    _uploadResult.postValue(Result.failure(Exception("请求出错")))
                }
            } catch (e: Exception) {
                Log.e("主页", "uploadFaceImage: ${e.message}")
            }
        }
    }

    /**
     * 与会人员信息校验
     */
    fun verifyUserByMeeting(userID: Long, meetingID: Long) {
        val nonce = Random().nextInt(10000).toString()
        val timestamp = (System.currentTimeMillis()).toString()
        val sign = CommonUtils.md5(AppConfig.SECRET_KEY + timestamp + nonce)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = JudgeRequest(userID, meetingID)
                val response =
                    apiService.accessControlJudge(nonce, timestamp, AppConfig.APP_ID, sign, request)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        if (it.code == 0) {
                            _recognitionResult.postValue(Result.success(body.data))
                        } else {
                            _recognitionResult.postValue(Result.failure(Exception("${it.message}")))
                        }
                    } ?: _recognitionResult.postValue(Result.failure(Exception("请求出错")))
                } else {
                    _recognitionResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {
                Log.e("主页", "verifyUserByMeeting: ${e.message}")
            }
        }
    }

    /**
     * 远程设置门禁状态
     * channel_ID:门禁设备id
     * door_status:
     * 1 常开
     * 2 常闭
     * 3 开
     * 4 关
     * 5 正常
     */
    fun settingDoorStatus(channel_ID: String, door_status: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ids = listOf<String>(channel_ID)
                val request = DoorStatusRequest(ids, door_status)
                val response = apiService.setDoorStatus(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        _doorResult.postValue(Result.success("请求成功"))
                    } ?: _doorResult.postValue(Result.failure(Exception("请求出错")))
                } else {
                    _doorResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {
                Log.e("主页", "verifyUserByMeeting: ${e.message}")
            }
        }
    }
}