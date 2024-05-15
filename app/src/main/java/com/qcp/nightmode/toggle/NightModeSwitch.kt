package com.qcp.nightmode.toggle

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.math.MathUtils.lerp

/**
 * @author chienpham
 * @since 15/05/2024
 */
@Suppress("SameParameterValue")
class NightModeSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var checked: Boolean = false
    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    private val backgroundPaint = Paint()
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = ContextCompat.getColor(context, R.color.transparent)
    }

    private val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.background)
    private val glowDrawable = ContextCompat.getDrawable(context, R.drawable.glow)
    private val sunDrawable = ContextCompat.getDrawable(context, R.drawable.sun)
    private val moonDrawable = ContextCompat.getDrawable(context, R.drawable.moon)

    private var offset: Float = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        addUpdateListener { animation ->
            offset = animation.animatedValue as Float
            invalidate()
        }
    }

    init {
        setOnClickListener {
            checked = !checked
            animateSwitch {
                onCheckedChangeListener?.invoke(checked)
            }
        }
    }

    fun setChecked(checked: Boolean, animate: Boolean = true) {
        if (this.checked != checked) {
            this.checked = checked
            if (animate) {
                animateSwitch()
            } else {
                // Immediate switch without animation
                offset = if (checked) 1f else 0f
                invalidate()
            }
        }
    }

    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        this.onCheckedChangeListener = listener
    }

    private fun animateSwitch(onCompletion: (() -> Unit)? = null) {
        animator.cancel() // Cancel any running animations before starting a new one
        if (checked) {
            animator.setFloatValues(offset, 1f)
        } else {
            animator.setFloatValues(offset, 0f)
        }
        animator.removeAllListeners()
        animator.addUpdateListener { animation ->
            offset = animation.animatedValue as Float
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onCompletion?.invoke()
            }
        })
        animator.start()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val switchWidth = width.toFloat()
        val switchHeight = height.toFloat()
        val handleSize = switchHeight - 20
        val handlePadding = 10f

        val cornerRadius = switchHeight / 2

        // Define a path for the rounded rectangle
        val path = Path().apply {
            addRoundRect(
                0f, 0f, switchWidth, switchHeight,
                cornerRadius, cornerRadius,
                Path.Direction.CW
            )
        }

        // Clip the canvas to the rounded rectangle path
        canvas.save()
        canvas.clipPath(path)

        // Draw background
        backgroundPaint.color = lerpColor(BlueSky, NightSky, offset)
        canvas.drawRoundRect(
            0f,
            0f,
            switchWidth,
            switchHeight,
            switchHeight / 2,
            switchHeight / 2,
            backgroundPaint
        )

        // Draw border
        canvas.drawRoundRect(
            0f,
            0f,
            switchWidth,
            switchHeight,
            switchHeight / 2,
            switchHeight / 2,
            borderPaint
        )

        // Draw background drawable
        backgroundDrawable?.let {
            it.setBounds(0, 0, switchWidth.toInt(), switchHeight.toInt())
            it.draw(canvas)
        }

        // Draw glow
        glowDrawable?.let {
            val left = lerp(
                -switchWidth / 2 + handlePadding + handleSize / 2,
                switchWidth - switchWidth / 2 - handlePadding - handleSize / 2,
                offset
            ).toInt()
            it.setBounds(left, 0, left + switchWidth.toInt(), switchHeight.toInt())
            it.draw(canvas)
        }

        // Draw sun
        sunDrawable?.let {
            val sunLeft = if (checked) {
                // Sun is outside the left when in night mode
                (-handleSize - handlePadding).toInt()
            } else {
                // Sun is inside the toggle when in light mode
                (handlePadding + (switchWidth - handleSize - handlePadding * 2) * offset).toInt()
            }
            it.setBounds(
                sunLeft,
                handlePadding.toInt(),
                sunLeft + handleSize.toInt(),
                handlePadding.toInt() + handleSize.toInt()
            )
            it.draw(canvas)
        }

        // Draw moon
        moonDrawable?.let {
            val moonLeft = if (checked) {
                // Moon is inside the toggle when in night mode
                (handlePadding + (switchWidth - handleSize - handlePadding * 2) * offset).toInt()
            } else {
                // Moon is outside the right when in light mode
                (switchWidth + handlePadding).toInt()
            }
            it.setBounds(
                moonLeft,
                handlePadding.toInt(),
                moonLeft + handleSize.toInt(),
                handlePadding.toInt() + handleSize.toInt()
            )
            it.draw(canvas)
        }

        // Draw border after restoring the canvas
        canvas.restore()
        canvas.drawRoundRect(
            0f, 0f, switchWidth, switchHeight,
            cornerRadius, cornerRadius,
            borderPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun lerpColor(colorStart: Int, colorEnd: Int, fraction: Float): Int {
        val startA = (colorStart shr 24) and 0xff
        val startR = (colorStart shr 16) and 0xff
        val startG = (colorStart shr 8) and 0xff
        val startB = colorStart and 0xff

        val endA = (colorEnd shr 24) and 0xff
        val endR = (colorEnd shr 16) and 0xff
        val endG = (colorEnd shr 8) and 0xff
        val endB = colorEnd and 0xff

        return ((startA + (fraction * (endA - startA)).toInt()) shl 24) or
                ((startR + (fraction * (endR - startR)).toInt()) shl 16) or
                ((startG + (fraction * (endG - startG)).toInt()) shl 8) or
                (startB + (fraction * (endB - startB)).toInt())
    }

    companion object {
        private const val BlueSky = 0xFF87CEEB.toInt() // Replace with actual color value
        private const val NightSky = 0xFF191970.toInt() // Replace with actual color value
    }
}
