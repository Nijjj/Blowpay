package com.example.minimalupi

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3121A24")
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.parseColor("#99D4F3FF")
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(4f)
        color = Color.parseColor("#FFF3FBFF")
    }

    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.parseColor("#B3B7F7FF")
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(10f)
        color = Color.parseColor("#2AB7F7FF")
    }

    private var scanProgress = 0f
    private val cutout = RectF()

    private val scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2200L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener {
            scanProgress = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!scanAnimator.isStarted) {
            scanAnimator.start()
        }
    }

    override fun onDetachedFromWindow() {
        scanAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val side = min(width, height) * 0.58f
        val left = (width - side) / 2f
        val top = height * 0.24f
        val radius = dp(24f)
        cutout.set(left, top, left + side, top + side)

        val checkpoint = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRoundRect(cutout, radius, radius, clearPaint)
        canvas.restoreToCount(checkpoint)

        canvas.drawRoundRect(cutout, radius, radius, glowPaint)
        canvas.drawRoundRect(cutout, radius, radius, framePaint)
        drawCorners(canvas, cutout)

        val lineY = cutout.top + cutout.height() * scanProgress
        canvas.drawLine(cutout.left + dp(14f), lineY, cutout.right - dp(14f), lineY, scanLinePaint)
    }

    private fun drawCorners(canvas: Canvas, rect: RectF) {
        val corner = dp(28f)

        canvas.drawLine(rect.left + dp(6f), rect.top + corner, rect.left + dp(6f), rect.top + dp(6f), cornerPaint)
        canvas.drawLine(rect.left + dp(6f), rect.top + dp(6f), rect.left + corner, rect.top + dp(6f), cornerPaint)

        canvas.drawLine(rect.right - dp(6f), rect.top + corner, rect.right - dp(6f), rect.top + dp(6f), cornerPaint)
        canvas.drawLine(rect.right - dp(6f), rect.top + dp(6f), rect.right - corner, rect.top + dp(6f), cornerPaint)

        canvas.drawLine(rect.left + dp(6f), rect.bottom - corner, rect.left + dp(6f), rect.bottom - dp(6f), cornerPaint)
        canvas.drawLine(rect.left + dp(6f), rect.bottom - dp(6f), rect.left + corner, rect.bottom - dp(6f), cornerPaint)

        canvas.drawLine(rect.right - dp(6f), rect.bottom - corner, rect.right - dp(6f), rect.bottom - dp(6f), cornerPaint)
        canvas.drawLine(rect.right - dp(6f), rect.bottom - dp(6f), rect.right - corner, rect.bottom - dp(6f), cornerPaint)
    }

    fun getCutoutRect(): RectF = RectF(cutout)

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
