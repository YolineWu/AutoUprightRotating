package com.yoline.autouprightrotating

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.children
import com.yoline.autouprightrotating.rotation.UprightDegrees
import com.yoline.autouprightrotating.rotation.UprightRotation
import com.yoline.autouprightrotating.rotation.UprightRotationCalculator.Companion.pivotRotation
import java.util.logging.Logger

open class UprightRotatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val logger: Logger = Logger.getLogger("yoline")

    lateinit var targetView: View
        private set
    var rotatingDuration: Long = DEFAULT_ROTATING_DURATION

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.UprightRotatingView, defStyleAttr, defStyleRes
        ).apply {
            rotatingDuration = getInt(
                R.styleable.UprightRotatingView_rotatingDuration, DEFAULT_ROTATING_DURATION.toInt()
            ).toLong()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        logger.info("onLayout")
        require(childCount == 1) { "There can be exactly one child view，there are currently $childCount" }
        targetView = children.first().apply {
            pivotX = width / 2f
            pivotY = height / 2f
        }
        require((targetView.layoutParams as LayoutParams).gravity == Gravity.CENTER) { "子view的layout_gravity必须是center" }
    }

    open fun childRotationBy(@UprightDegrees degrees: Int) {
        UprightRotation.byDegrees(degrees)?.let {
            childRotationBy(it)
        } ?: throw IllegalArgumentException("degrees 只能是-90、0、90或180，当前为$degrees")
    }

    open fun childRotationBy(rotation: UprightRotation) {
        if (rotatingDuration == 0L) {
            targetView.pivotRotation()?.let {
                targetView.rotation = it.plus(rotation).degrees.toFloat()
            }
            swapTargetWidthHeight()
            return
        }
        targetView.animate().apply {
            duration = rotatingDuration
            if (rotation.is90) withStartAction { swapTargetWidthHeight() }
        }.rotationBy(rotation.degrees.toFloat())
    }

    open fun childRotation(@UprightDegrees degrees: Int) {
        UprightRotation.byDegrees(degrees)?.let {
            childRotation(it)
        } ?: throw IllegalArgumentException("degrees 只能是-90、0、90或180，当前为$degrees")
    }

    open fun childRotation(rotation: UprightRotation) {
        targetView.pivotRotation(0f)?.let {
            childRotationBy(rotation.minus(it))
        } ?: throw IllegalArgumentException("调用此方法时，targetView的rotation必须是90的倍数")
    }

    private fun swapTargetWidthHeight() {
        if (rotatingDuration == 0L) {
            targetView.layoutParams = targetView.layoutParams.apply {
                height = targetView.width
                width = targetView.height
            }
            return
        }
        val heightAnim = ValueAnimator.ofInt(targetView.height, targetView.width).apply {
            addUpdateListener {
                targetView.layoutParams = targetView.layoutParams.apply {
                    height = it.animatedValue as Int
                }
            }
        }
        val widthAnim = ValueAnimator.ofInt(targetView.width, targetView.height).apply {
            addUpdateListener {
                targetView.layoutParams = targetView.layoutParams.apply {
                    width = it.animatedValue as Int
                }
            }

        }
        AnimatorSet().apply {
            play(heightAnim).with(widthAnim)
            duration = rotatingDuration
            start()
        }
    }

    companion object {
        const val DEFAULT_ROTATING_DURATION = 300L
    }
}