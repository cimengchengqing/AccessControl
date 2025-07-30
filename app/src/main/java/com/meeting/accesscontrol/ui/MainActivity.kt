package com.meeting.accesscontrol.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.meeting.accesscontrol.adapter.MeetingListAdapter
import com.meeting.accesscontrol.aotu_launch.AutoStartPermissionHelper
import com.meeting.accesscontrol.bean.Meeting
import com.meeting.accesscontrol.bean.MeetingRoom
import com.meeting.accesscontrol.databinding.ActivityMainBinding
import com.meeting.accesscontrol.net.ApiService
import com.meeting.accesscontrol.net.NetworkManager
import com.meeting.accesscontrol.tools.LogUtils
import com.meeting.accesscontrol.tools.TimeFormatter
import com.meeting.accesscontrol.view.CustomToast
import com.meeting.accesscontrol.view.FaceScanMaskView
import com.meeting.accesscontrol.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var orderMeetings = mutableListOf<Meeting>()    //预约的会议列表（除去当前正在进行的会议）
    private var currMeeting: Meeting? = null
    private var roomDoorId: String? = null  // 会议室门禁设备ID
    private lateinit var adapter: MeetingListAdapter

    private val apiService by lazy {
        NetworkManager.getInstance(applicationContext).createService(ApiService::class.java)
    }
    private lateinit var mViewModel: MainViewModel

    //时间系统
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeRunnable: Runnable

    private lateinit var maskView: FaceScanMaskView // 获取遮罩View的引用
    private lateinit var statusTextView: TextView   //状态提示的view
    private lateinit var previewView: PreviewView
    private lateinit var preview: Preview
    private var cameraSelector: CameraSelector? = null
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraExecutor: ExecutorService? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var analysisActive = false
    private var cameraInitiated = false
    private lateinit var faceDetector: FaceDetector

    // 状态管理变量
    @Volatile
    private var isCapturing = false // 是否正在处理拍照，防止重复触发
    private var stableFaceCounter = 0 // 人脸稳定帧数计数器
    private val REQUIRED_STABLE_FRAMES = 5 // 需要稳定多少帧才拍照
    private val deviceID: String by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private lateinit var autoStartPermissionHelper: AutoStartPermissionHelper
    private val mToast: CustomToast = CustomToast.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.d(TAG, "onCreate: ")
        hideVirtualKey()

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mViewModel = MainViewModel(apiService)
        LogUtils.d(TAG, "ANDROID_ID: $deviceID")
        //检查权限
        if (!allPermissionsGranted()) {
            // 请求权限
            LogUtils.d("MainActivity", "onCreate: 去请求拍照权限")
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        initData()
        initViews()
        initEvents()
    }

    /**
     * 检查自启动权限
     */
    private fun checkAutoStartPermission() {
        try {
            val hasPermission = autoStartPermissionHelper.checkAutoStartPermission()
            val message = if (hasPermission) "自启动权限已授予" else "自启动权限未授予"
            LogUtils.d(TAG, "自启动权限检查结果: $message")

        } catch (e: Exception) {
            LogUtils.e(TAG, "检查自启动权限失败", e)
        }
    }

    /**
     * 测试开机启动
     */
//    private fun testBootStartup() {
//        try {
//            LogUtils.d(TAG, "测试开机启动")
//
//            // 模拟开机启动
//            BootStartupManager.getInstance(this).startAppImmediately()
//
//            CustomToast.getInstance().showSuccess(this, "测试完成")
//
//        } catch (e: Exception) {
//            LogUtils.e(TAG, "测试开机启动失败", e)
//            updateStatus("测试失败: ${e.message}")
//            CustomToast.getInstance().showError(this, "测试失败")
//        }
//    }

    private fun initEvents() {
        binding.cancelButton.setOnClickListener {
            stopFaceAnalysis()
        }

        binding.faceIdentificationRl.setOnClickListener {
            startFaceAnalysis()
        }

        mViewModel.meetingResult.observe(this) { result ->
            result.onSuccess { bean ->
                LogUtils.d(TAG, "请求会议信息成功 ")
                updateMeetingStatus(bean)
            }.onFailure { e ->
                mToast.showError(applicationContext, "获取会议信息失败")
            }
        }

        mViewModel.uploadResult.observe(this) { result ->
            result.onSuccess { id ->
                // 人脸识别完成，拿到用户id和会议id，校验是否能够参加会议
                try {
                    currMeeting?.let {
                        mViewModel.verifyUserByMeeting(id.toLong(), it.meetingId.toLong())
                    } ?: run {
                        mToast.showError(applicationContext, "没有进行中的会议")
                        stopFaceAnalysis()
                        isCapturing = false
                    }
                } catch (e: Exception) {
                    mToast.showError(applicationContext, "请求出错")
                    isCapturing = false
                }
            }.onFailure { e ->
                mToast.showError(applicationContext, "人脸识别失败")
                isCapturing = false
            }
        }

        mViewModel.recognitionResult.observe(this) { result ->
            result.onSuccess { id ->
                //识别成功，请求打开门禁
                roomDoorId?.let {
                    mViewModel.settingDoorStatus(it, 3)
                }
            }.onFailure { e ->
                mToast.showError(applicationContext, "人脸识别失败")
            }
            isCapturing = false
        }

        mViewModel.doorResult.observe(this) { result ->
            result.onSuccess { msg ->
                //门禁已打开
                mToast.showSuccess(applicationContext, "人脸识别成功")
            }.onFailure { e ->
                //门禁打开失败
                mToast.showError(applicationContext, "人门禁开启失败")
            }
            stopFaceAnalysis()
            isCapturing = false
        }
    }

    /**根据所有的会议信息更新当前会议状态
     * 1、校验是否有会议在时间段在当前时间
     * 2、更具具体情况展示当前会与会议或者下场会议
     */
    private fun updateMeetingStatus(bean: MeetingRoom) {
        LogUtils.d(TAG, "updateMeetingStatus: ")

        roomDoorId = bean.deviceId

        runOnUiThread {
            bean.roomName?.let {
                binding.meetingRoomTv.text = it
            }

            if (bean.meetingInfoList.isEmpty()) {
                binding.meetingStatuesTv.text = "空闲中"
                binding.meetingContentLl.visibility = View.GONE
                binding.faceIdentificationRl.visibility = View.VISIBLE
                binding.nullOrderRl.visibility = View.VISIBLE
                binding.orderList.visibility = View.GONE
            } else {
                binding.meetingContentLl.visibility = View.VISIBLE

                orderMeetings.clear()

                //会议按开始时间升序排序，并过滤掉已过期的会议
                val currTime = System.currentTimeMillis()
                val data =
                    bean.meetingInfoList
                        .sortedBy { it.startTime }
//                        .filter { meeting: Meeting ->
//                            meeting.endTime > currTime
//                        }
                orderMeetings.addAll(data)  //如果第一条会议在20分钟以内开始或者已经开始，不需要展示到“今日预约”

                //获取第一条会议信息进行处理
                currMeeting = bean.meetingInfoList[0]
                val firstMeeting = currMeeting!!
                val timeNum = (firstMeeting.startTime - System.currentTimeMillis()) / 1000 / 60
                val statusText = when {
                    timeNum > 20 -> {
                        binding.meetingTitleTv.text = "下一场会议：${firstMeeting.title}"
                        binding.meetingTimeTv.text =
                            "会议时间：${TimeFormatter.getHoursForTimestamp(firstMeeting.startTime)} 开始"
                        binding.meetingInitiatorTv.text = "发起人：${firstMeeting.promoter}"
                        binding.meetingParticipantTv.visibility = View.GONE
                        binding.faceIdentificationRl.visibility = View.GONE

                        "空闲中"
                    }

                    timeNum > 0 -> {
                        binding.meetingTitleTv.text = "会议主题：${firstMeeting.title}"
                        binding.meetingTimeTv.text =
                            "会议时间：${
                                TimeFormatter.formatSameDayTimeRange(
                                    firstMeeting.startTime,
                                    firstMeeting.endTime
                                )
                            }"
                        binding.meetingInitiatorTv.text = "发起人：${firstMeeting.promoter}"
                        val peoples = firstMeeting.userInfoList.map { it.name }.joinToString("、")
//                        binding.meetingParticipantTv.text = "参会人员：李总、陈总、张总、徐总、朱总、李总、陈总、张总、徐总、朱总、李总、陈总、张总、徐总、朱总"
                        binding.meetingParticipantTv.text = "参会人员：$peoples"

                        binding.meetingParticipantTv.visibility = View.VISIBLE
                        binding.faceIdentificationRl.visibility = View.VISIBLE

                        orderMeetings.removeAt(0)
                        LogUtils.d(TAG, "移除数据（会议待开始），会议长度=${orderMeetings.size}")

                        "会议待开始"
                    }

                    else -> {
                        binding.meetingTitleTv.text = "会议主题：${firstMeeting.title}"
                        binding.meetingTimeTv.text =
                            "会议时间：${
                                TimeFormatter.formatSameDayTimeRange(
                                    firstMeeting.startTime,
                                    firstMeeting.endTime
                                )
                            }"
                        binding.meetingInitiatorTv.text = "发起人：${firstMeeting.promoter}"
                        val peoples = firstMeeting.userInfoList.map { it.name }.joinToString("、")
                        binding.meetingParticipantTv.text = "参会人员：$peoples"
                        binding.meetingParticipantTv.visibility = View.VISIBLE
                        binding.faceIdentificationRl.visibility = View.VISIBLE

                        orderMeetings.removeAt(0)
                        LogUtils.d(TAG, "移除数据（会议进行中），会议长度=${orderMeetings.size}")

                        "会议进行中"
                    }
                }
                binding.meetingStatuesTv.text = statusText

                //处理预约会议列表展示逻辑
                if (orderMeetings.isNullOrEmpty()) {
                    binding.nullOrderRl.visibility = View.VISIBLE
                    binding.orderList.visibility = View.GONE
                } else {
                    binding.nullOrderRl.visibility = View.GONE
                    binding.orderList.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "未授予相机权限", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LogUtils.d(TAG, "onResume: ")
        mViewModel.requestMeetingInfo(deviceID)
        checkAutoStartPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "onDestroy: ")
        cameraExecutor?.shutdown()
        faceDetector.close()
        handler.removeCallbacks(timeRunnable) // 防止内存泄漏
    }

    // 检查权限
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun initViews() {
        maskView = binding.maskView
        statusTextView = binding.inputHint
        previewView = binding.cameraPreview

        initCamera()
    }

    private fun initData() {
        adapter = MeetingListAdapter(applicationContext, orderMeetings)
        binding.orderList.adapter = adapter

        timeRunnable = object : Runnable {
            override fun run() {
                val (time, date) = getCurrentTimeAndDate()
                val currTime = binding.timeTv.text
                if (!currTime.equals("") && time != currTime) {
                    mViewModel.requestMeetingInfo(deviceID)
                }
                binding.timeTv.text = time
                binding.dateTv.text = date
                handler.postDelayed(this, 1000) // 每秒刷新一次
            }
        }

        handler.post(timeRunnable) // 启动定时刷新
    }

    /**
     * 初始化相机相关配置
     */
    private fun initCamera() {
        //配置人脸检测参数（可选，高精度模式/轮廓模式等）
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL).enableTracking().build()
        faceDetector = FaceDetection.getClient(options)

        // 初始化 Preview 对象，在该对象上调用 build，从取景器中获取表面提供程序，然后在预览中进行设置。
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture =
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

        // 配置图像分析
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    LogUtils.d(TAG, "Analyzer 收到回调")
                    processImageProxy(imageProxy)
                }
            }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // 将相机的生命周期绑定到生命周期所有者.消除了打开和关闭相机的任务，因为 CameraX 具有生命周期感知能力
            cameraProvider = cameraProviderFuture.get()

            // 优先使用前置摄像头（人脸识别）
            cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    LogUtils.d(TAG, "前置摄像头")
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    LogUtils.d(TAG, "后置摄像头")
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    null
                }
            cameraInitiated = true
        }, ContextCompat.getMainExecutor(this)) // 回调代码在主线程处理
    }

    /**
     * 开启人脸分析
     */
    private fun startFaceAnalysis() {
        if (cameraInitiated && !analysisActive && cameraSelector != null) {
            analysisActive = true
            cameraProvider.let {
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector!!, preview, imageCapture, imageAnalysis
                    )
                    LogUtils.d(TAG, "初始化完成")
                    binding.faceInputContainer.visibility = View.VISIBLE
                    binding.orderContainer.visibility = View.GONE
                } catch (exc: Exception) {
                    LogUtils.e(TAG, "相机绑定失败", exc)
                    mToast.showError(this, "相机绑定失败")
                }
            }
        } else {
            mToast.showError(applicationContext, "相机未准备好")
        }
    }

    /**
     * 暂停人脸分析
     */
    private fun stopFaceAnalysis() {
        cameraProvider.unbindAll()
        analysisActive = false
        binding.faceInputContainer.visibility = View.GONE
        binding.orderContainer.visibility = View.VISIBLE
    }

    /**
     * 初始化相机
     * 使用 CameraX API进行预览
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // 1、将相机的生命周期绑定到生命周期所有者.消除了打开和关闭相机的任务，因为 CameraX 具有生命周期感知能力
            cameraProvider = cameraProviderFuture.get()

            // 2、初始化您的 Preview 对象，在该对象上调用 build，从取景器中获取表面提供程序，然后在预览中进行设置。
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture =
                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

            // 配置图像分析
            cameraExecutor = Executors.newSingleThreadExecutor()
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                    it.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        LogUtils.d(TAG, "Analyzer 收到回调")
                        processImageProxy(imageProxy)
                    }
                }

            // 优先使用前置摄像头（人脸识别）
            val cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    LogUtils.d(TAG, "前置摄像头")
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    LogUtils.d(TAG, "后置摄像头")
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    mToast.showError(this, "设备没有可用摄像头")
                    return@addListener
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis
                )
                LogUtils.d(TAG, "相机初始化成功")
            } catch (exc: Exception) {
                LogUtils.e(TAG, "相机绑定失败", exc)
                mToast.showError(this, "相机绑定失败")
            }
        }, ContextCompat.getMainExecutor(this)) // 回调代码在主线程处理
    }


    /**
     * 照片分析
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        // 如果正在拍照，则跳过分析
        if (isCapturing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            faceDetector.process(image).addOnSuccessListener { faces ->
                checkFaceForAutoCapture(faces)
            }.addOnFailureListener { e ->
                updateStatus("检测失败")
                stableFaceCounter = 0
            }.addOnCompleteListener {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    /**
     * 检查是否满足自动拍照的条件
     */
    private fun checkFaceForAutoCapture(faces: List<Face>) {
        // 1. 条件一：画面中必须只有一张人脸
        if (faces.size != 1) {
            updateStatus("请确保只有一张人脸")
            stableFaceCounter = 0
            return
        }

        val face = faces[0]

        // 2. 条件二：人脸必须在遮罩内且大小合适
        if (!isFaceInMask(face)) {
            updateStatus("请将人脸移入圆形区域中央")
            stableFaceCounter = 0
            return
        }

        // 3. 条件三：双眼必须睁开
//        if (face.leftEyeOpenProbability ?: 0f < 0.5 || face.rightEyeOpenProbability ?: 0f < 0.5) {
//            updateStatus("请睁开双眼")
//            stableFaceCounter = 0
//            return
//        }

        // 所有条件都满足，开始稳定计数
        stableFaceCounter++
        updateStatus("很好，请保持稳定...")

        // 4. 条件四：稳定帧数达标
        if (stableFaceCounter >= REQUIRED_STABLE_FRAMES) {
            // 满足所有条件，执行拍照
            isCapturing = true // 设置状态为正在拍照
            stableFaceCounter = 0
            updateStatus("正在采集中...")
            takePhoto()
        }
    }

    /**
     * 检查人脸是否在遮罩圆形区域内
     */
    private fun isFaceInMask(face: Face): Boolean {
        // 获取人脸的边界框
        val faceBox = face.boundingBox
        // 获取遮罩View的中心和半径
        // 注意：这里直接引用了 maskView 的属性，确保 maskView 已经 onSizeChanged
        val maskCenterX = maskView.width / 2f
        val maskCenterY = maskView.height / 2f
        val maskRadius = minOf(maskView.width, maskView.height) / 3f

        // 计算人脸中心点和遮罩中心点的距离
        val distance = Math.sqrt(
            Math.pow(
                (faceBox.centerX() - maskCenterX).toDouble(), 2.0
            ) + Math.pow((faceBox.centerY() - maskCenterY).toDouble(), 2.0)
        ).toFloat()

        // 检查人脸中心是否在圆形区域内，并且人脸宽度小于圆形直径
        val faceWidth = faceBox.width()
        return distance < maskRadius && faceWidth < maskRadius * 2
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    mViewModel.uploadFaceImage(bitmap)
                    updateStatus("采集成功，正在识别")
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        baseContext, "拍照失败: ${exception.message}", Toast.LENGTH_SHORT
                    ).show()
                    updateStatus("采集失败，请重试")
                    isCapturing = false
                }
            })
    }

    /**
     * 更新人脸录制提示状态
     */
    private fun updateStatus(text: String) {
        runOnUiThread {
            statusTextView.text = text
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val TAG: String = "主页"

        /**
         * 获取当前时间和日期字符串
         * @return Pair<时间字符串, 日期字符串>
         */
        fun getCurrentTimeAndDate(): Pair<String, String> {
            val calendar = Calendar.getInstance()

            // 时间格式：15:50
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStr = timeFormat.format(calendar.time)

            // 日期格式：2020 年 12 月 20 日
            val dateFormat = SimpleDateFormat("yyyy 年 MM 月 dd 日", Locale.getDefault())
            val dateStr = dateFormat.format(calendar.time)

            // 星期
            val weekDays =
                arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
            val weekDayStr = weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]

            // 拼接日期和星期
            val dateWithWeek = "$dateStr    $weekDayStr"

            return Pair(timeStr, dateWithWeek)
        }
    }

}

private fun MainActivity.hideVirtualKey() {
    val decorView = getWindow().getDecorView()
    val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    decorView.setSystemUiVisibility(uiOptions)
}