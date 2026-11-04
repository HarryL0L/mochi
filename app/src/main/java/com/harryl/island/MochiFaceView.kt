package com.harryl.island

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.PathInterpolator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * Mochi's face, drawn live with the same shapes as the launcher icon (108-unit viewport).
 * Tap it and Mochi winks: the right eye closes into a happy arc, the blush warms up and the
 * whole face squishes and springs back. Long press for a double "^^" smile.
 */
class MochiFaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEDE7FF.toInt() }
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF16161D.toInt() }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val blush = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF8FB1.toInt() }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()
    private val path = Path()

    /** 1 = open, 0 = closed, per eye. */
    private var leftEye = 1f
    private var rightEye = 1f
    private var blushBoost = 0f
    private var running: ValueAnimator? = null

    private val squishX = SpringAnimation(this, DynamicAnimation.SCALE_X, 1f).apply {
        spring = SpringForce(1f).setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(0.45f)
    }
    private val squishY = SpringAnimation(this, DynamicAnimation.SCALE_Y, 1f).apply {
        spring = SpringForce(1f).setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(0.45f)
    }

    init {
        isClickable = true
        contentDescription = context.getString(R.string.face_description)
        setOnClickListener { wink(bothEyes = false) }
        setOnLongClickListener { wink(bothEyes = true); true }
    }

    fun wink(bothEyes: Boolean) {
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        // Squish: wider and shorter, then spring back.
        scaleX = 1.12f
        scaleY = 0.88f
        squishX.start()
        squishY.start()
        running?.cancel()
        running = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (bothEyes) 900 else 650
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                val t = it.animatedFraction
                // Close fast, hold, reopen: 0..0.2 close, 0.2..0.7 hold, 0.7..1 open.
                val open = when {
                    t < 0.2f -> 1 - t / 0.2f
                    t < 0.7f -> 0f
                    else -> (t - 0.7f) / 0.3f
                }
                rightEye = open
                if (bothEyes) leftEye = open
                blushBoost = 1 - open
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val k = minOf(width, height) / 108f
        canvas.save()
        canvas.translate((width - 108 * k) / 2, (height - 108 * k) / 2)
        canvas.scale(k, k)

        rect.set(0f, 0f, 108f, 108f)
        canvas.drawRoundRect(rect, 30f, 30f, bg)
        rect.set(24f, 38f, 84f, 70f)
        canvas.drawRoundRect(rect, 16f, 16f, body)

        blush.alpha = (150 + 90 * blushBoost).toInt()
        val br = 1f + 0.25f * blushBoost
        rect.set(38.5f - 4 * br, 60 - 2.6f * br, 38.5f + 4 * br, 60 + 2.6f * br); canvas.drawOval(rect, blush)
        rect.set(69.5f - 4 * br, 60 - 2.6f * br, 69.5f + 4 * br, 60 + 2.6f * br); canvas.drawOval(rect, blush)

        drawEye(canvas, 45f, leftEye)
        drawEye(canvas, 63f, rightEye)

        // Smile grows a little while winking.
        stroke.strokeWidth = 2f
        val lift = 1.2f * blushBoost
        path.reset()
        path.moveTo(50.5f - lift, 60.5f)
        path.quadTo(54f, 63.5f + lift, 57.5f + lift, 60.5f)
        canvas.drawPath(path, stroke)
        canvas.restore()
    }

    private fun drawEye(canvas: Canvas, cx: Float, open: Float) {
        if (open > 0.15f) {
            val h = 3.4f * open
            rect.set(cx - 3f, 52f - h, cx + 3f, 52f + h)
            canvas.drawOval(rect, white)
        } else {
            // Closed: a happy upward arc, like ^.
            stroke.strokeWidth = 1.8f
            path.reset()
            path.moveTo(cx - 3.2f, 53f)
            path.quadTo(cx, 49f, cx + 3.2f, 53f)
            canvas.drawPath(path, stroke)
        }
    }
}
