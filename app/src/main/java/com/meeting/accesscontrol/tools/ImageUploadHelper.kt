package com.meeting.accesscontrol.tools

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 图片上传工具类
 */
class ImageUploadHelper {

    companion object {
        private const val TAG = "ImageUploadHelper"
        private const val IMAGE_QUALITY = 80 // 图片压缩质量
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 最大图片大小 1MB
    }

   suspend fun bitmapToMultipartPart(
        bitmap: Bitmap,
        partName: String = "file",
        fileName: String = "face_image.jpg"
    ): MultipartBody.Part {
        // 1. Bitmap转为字节流
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        val byteArray = bos.toByteArray()
        bos.close()

        // 2. 构建RequestBody
        val requestBody =
            RequestBody.create("application/octet-stream".toMediaTypeOrNull(), byteArray)

        // 3. 构建MultipartBody.Part
        return MultipartBody.Part.createFormData(partName, fileName, requestBody)
    }

    /**
     * 将Bitmap转换为MultipartBody.Part
     */
    suspend fun bitmapToMultipartPart(
        bitmap: Bitmap,
        fileName: String = "face_image.jpg"
    ): MultipartBody.Part = withContext(Dispatchers.IO) {
        try {
            // 压缩Bitmap
            val compressedBitmap = compressBitmap(bitmap)

            // 将Bitmap转换为字节数组
            val byteArrayOutputStream = ByteArrayOutputStream()
            compressedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                IMAGE_QUALITY,
                byteArrayOutputStream
            )
            val imageBytes = byteArrayOutputStream.toByteArray()

            // 创建临时文件
            val tempFile = createTempFile(fileName, imageBytes)

            // 创建RequestBody
            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

            // 创建MultipartBody.Part
            MultipartBody.Part.createFormData("file", fileName, requestBody)

        } catch (e: Exception) {
            Log.e(TAG, "转换Bitmap失败: ${e.message}")
            throw e
        }
    }

    /**
     * 压缩Bitmap
     */
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        var compressedBitmap = bitmap

        // 如果图片太大，进行压缩
        val byteArrayOutputStream = ByteArrayOutputStream()
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, byteArrayOutputStream)

        var quality = IMAGE_QUALITY
        while (byteArrayOutputStream.size() > MAX_IMAGE_SIZE && quality > 10) {
            byteArrayOutputStream.reset()
            quality -= 10
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        }

        return compressedBitmap
    }

    /**
     * 创建临时文件
     */
    private fun createTempFile(fileName: String, imageBytes: ByteArray): File {
        val tempFile = File.createTempFile("upload_", "_$fileName")
        val fileOutputStream = FileOutputStream(tempFile)
        fileOutputStream.write(imageBytes)
        fileOutputStream.close()
        return tempFile
    }
}