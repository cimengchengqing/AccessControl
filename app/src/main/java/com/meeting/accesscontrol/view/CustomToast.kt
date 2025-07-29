package com.meeting.accesscontrol.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.meeting.accesscontrol.R

/**
 * 自定义Toast类
 * 支持成功和失败两种状态，使用自定义drawable背景
 */
class CustomToast private constructor() {

    companion object {
        private const val TOAST_DURATION = Toast.LENGTH_SHORT
        private const val TOAST_GRAVITY = Gravity.CENTER
        private const val TOAST_Y_OFFSET = 100

        @Volatile
        private var INSTANCE: CustomToast? = null

        fun getInstance(): CustomToast {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CustomToast().also { INSTANCE = it }
            }
        }
    }

    /**
     * Toast配置数据类
     */
    data class ToastConfig(
        val icon: Drawable? = null,
        val text: String = "",
        val backgroundDrawable: Drawable? = null,
        val textColor: Int = android.graphics.Color.WHITE,
    )

    /**
     * 显示成功Toast
     */
    fun showSuccess(context: Context, message: String, duration: Int = TOAST_DURATION) {
        val config = ToastConfig(
            icon = ContextCompat.getDrawable(context, R.drawable.icon_toast_success),
            text = message,
            backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.toast_success_bg),
            textColor = android.graphics.Color.WHITE,
        )
        showCustomToast(context, config, duration)
    }

    /**
     * 显示失败Toast
     */
    fun showError(context: Context, message: String, duration: Int = TOAST_DURATION) {
        val config = ToastConfig(
            icon = ContextCompat.getDrawable(context,R.drawable.icon_toast_fail),
            text = message,
            backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.toast_fail_bg),
            textColor = android.graphics.Color.WHITE,
        )
        showCustomToast(context, config, duration)
    }

    /**
     * 显示自定义Toast
     */
    private fun showCustomToast(
        context: Context,
        config: ToastConfig,
        duration: Int = TOAST_DURATION
    ) {
        try {
            // 创建自定义布局
            val toastView = createToastView(context, config)

            // 创建Toast
            val toast = Toast(context)
            toast.view = toastView
            toast.duration = duration
            toast.setGravity(TOAST_GRAVITY, 0, TOAST_Y_OFFSET)

            // 显示Toast
            toast.show()
        } catch (e: Exception) {
            // 降级到系统Toast
            Toast.makeText(context, config.text, duration).show()
        }
    }

    /**
     * 创建Toast视图
     */
    private fun createToastView(context: Context, config: ToastConfig): View {
        // 创建根布局
        val linearLayout = android.widget.LinearLayout(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL

            // 设置自定义背景
            config.backgroundDrawable?.let { drawable ->
                background = drawable
            }

            // 设置内边距
            setPadding(20, 8, 20, 8)
        }

        // 创建图标
        val iconView = ImageView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }

            config.icon?.let { drawable ->
                setImageDrawable(drawable)
            }

            // 设置图标大小
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            maxWidth = 18
            maxHeight = 18
        }

        // 创建文本
        val textView = TextView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = config.text
            setTextColor(config.textColor)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT
        }

        // 组装视图
        linearLayout.addView(iconView)
        linearLayout.addView(textView)

        return linearLayout
    }

    /**
     * 根据网络请求结果显示Toast
     */
    fun showNetworkResult(context: Context, isSuccess: Boolean, message: String) {
        if (isSuccess) {
            showSuccess(context, message)
        } else {
            showError(context, message)
        }
    }
}