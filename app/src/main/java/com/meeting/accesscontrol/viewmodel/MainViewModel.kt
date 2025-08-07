package com.meeting.accesscontrol.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meeting.accesscontrol.bean.JudgeRequest
import com.meeting.accesscontrol.bean.MeetingRoom
import com.meeting.accesscontrol.bean.RoomInfo
import com.meeting.accesscontrol.bean.TokenNewBean
import com.meeting.accesscontrol.bean.TokenRefreshBean
import com.meeting.accesscontrol.net.ApiService
import com.meeting.accesscontrol.net.ApiService.DoorStatusRequest
import com.meeting.accesscontrol.tools.AppConfig
import com.meeting.accesscontrol.tools.CommonUtils
import com.meeting.accesscontrol.tools.ImageUploadHelper
import com.meeting.accesscontrol.tools.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.util.Random

class MainViewModel(private val apiService: ApiService) : ViewModel() {

    private val _typeResult = MutableLiveData<Result<RoomInfo>>()
    val typeResult: LiveData<Result<RoomInfo>> = _typeResult

    private val _tokenResult = MutableLiveData<Result<TokenNewBean>>()
    val tokenResult: LiveData<Result<TokenNewBean>> = _tokenResult

    private val _refreshResult = MutableLiveData<Result<TokenRefreshBean>>()
    val refreshResult: LiveData<Result<TokenRefreshBean>> = _refreshResult

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
     * 请求平板对应的房间类型:
     * 1    会议室
     * 2    办公室
     */
    fun requestRoomType(deviceID: String) {
        val nonce = Random().nextInt(10000).toString()
        val timestamp = (System.currentTimeMillis()).toString()
        val sign = CommonUtils.md5(AppConfig.SECRET_KEY + timestamp + nonce)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.verifyRoomType(
                    nonce,
                    timestamp,
                    AppConfig.APP_ID,
                    sign,
                    ApiService.DeviceInfo(deviceID)
                )
                if (response.code() == 200 && response.body() != null && response.body()!!.code == 0) {
                    // 处理数据
                    _typeResult.postValue(Result.success(response.body()!!.data))
                } else {
                    _typeResult.postValue(Result.failure(Exception("请求出错")))
                }
            } catch (e: ConnectException) {
                // 捕获连接异常:ml-citation{ref="8" data="citationList"}
                LogUtils.d("主页", "网络不可达")
                _typeResult.postValue(Result.failure(Exception("网络访问失败")))
            } catch (e: Exception) {
                // 兜底处理
                LogUtils.d("主页", "请求出错")
                _typeResult.postValue(Result.failure(Exception("请求出错")))
            }
        }
    }

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
                LogUtils.e("主页", "requestMeetingInfo: ${e.message}")
                _meetingResult.postValue(Result.failure(Exception("请求失败")))
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
                LogUtils.e("主页", "uploadFaceImage: ${e.message}")
                _uploadResult.postValue(Result.failure(Exception("${e.message}")))
            }
        }
    }

    /**
     * 人员信息校验
     */
    fun verifyUserByID(userID: Long, meetingID: Long, deviceID: String) {
        val nonce = Random().nextInt(10000).toString()
        val timestamp = (System.currentTimeMillis()).toString()
        val sign = CommonUtils.md5(AppConfig.SECRET_KEY + timestamp + nonce)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = JudgeRequest(userID, meetingID, deviceID)
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
                _recognitionResult.postValue(Result.failure(Exception("${e.message}}")))
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
    fun settingDoorStatus(token: String, channel_ID: String, door_status: Int = 3) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ids = listOf<String>(channel_ID)
                val request = DoorStatusRequest(ids, door_status)
                val response = apiService.setDoorStatus(
                    token,
                    "usercode:${AppConfig.API_ACCOUNT}",
                    "usercode=${AppConfig.API_ACCOUNT}",
                    request
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.error_code?.let {
                        when (it.equals("0000000000")) {
                            true -> _doorResult.postValue(Result.success("请求成功"))
                            else -> _doorResult.postValue(Result.failure(Exception(body.message)))
                        }
                    } ?: _doorResult.postValue(Result.failure(Exception(body?.message)))
                } else {
                    _doorResult.postValue(Result.failure(Exception("未知原因")))
                }
            } catch (e: Exception) {
                _doorResult.postValue(Result.failure(Exception("未知原因")))
            }
        }
    }

    /**
     * 获取门禁认证Token
     */
    fun getAccessControlToken() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getToken()
                if (response.isSuccessful) {
                    if (response.code() == 200 && response.body() != null) {
                        _tokenResult.postValue(Result.success(response.body()!!))
                    } else {
                        _tokenResult.postValue(Result.failure(Exception("请求token出错")))
                    }
                } else {
                    _tokenResult.postValue(Result.failure(Exception("请求token出错")))
                }
            } catch (e: Exception) {
                // 兜底处理
                LogUtils.d("主页", "请求token出错")
                _tokenResult.postValue(Result.failure(Exception("请求token出错")))
            }
        }
    }

    /**
     * 刷新门禁认证Token
     */
    fun refreshControlToken(refreshToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.refreshToken(refresh_token = refreshToken)
                if (response.isSuccessful) {
                    if (response.code() == 200 && response.body() != null) {
                        _refreshResult.postValue(Result.success(response.body()!!))
                    } else {
                        _refreshResult.postValue(Result.failure(Exception("刷新token出错")))
                    }
                } else {
                    _refreshResult.postValue(Result.failure(Exception("刷新token出错")))
                }
            } catch (e: Exception) {
                // 兜底处理
                LogUtils.d("主页", "刷新token出错")
                _refreshResult.postValue(Result.failure(Exception("刷新token出错")))
            }
        }
    }
}