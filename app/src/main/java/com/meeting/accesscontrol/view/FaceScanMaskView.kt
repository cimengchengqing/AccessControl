package com.meeting.accesscontrol.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceScanMaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 背景遮罩画笔
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 半透明的黑色背景
        color = Color.parseColor("#A8000000")
    }

    // 镂空区域画笔
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 设置为 CLEAR 模式，用于擦除
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // 圆角矩形的圆角半径
    private val cornerRadius = 16f

    // 中间圆形镂空的半径
    private var circleRadius = 0f
    // 中间圆形镂空的中心点坐标
    private var circleCenterX = 0f
    private var circleCenterY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 当 View 尺寸变化时，重新计算圆形的位置和大小
        circleCenterX = w / 2f
        circleCenterY = h / 2f
        // 让圆形的半径为宽度和高度中较小者的 1/3
        circleRadius = minOf(w, h) / 4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 创建一个新的图层，后续的绘制操作将在这个图层上进行
        //    这是使用 PorterDuffXfermode 的关键步骤
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 2. 绘制背景：一个半透明的圆角矩形
        canvas.drawRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            cornerRadius,
            cornerRadius,
            backgroundPaint
        )

        // 3. 绘制镂空区域：使用 CLEAR 模式的画笔绘制一个圆形
        canvas.drawCircle(
            circleCenterX,
            circleCenterY,
            circleRadius,
            clearPaint
        )

        // 4. 将图层绘制到 View 上，完成混合
        canvas.restoreToCount(layerId)
    }
}