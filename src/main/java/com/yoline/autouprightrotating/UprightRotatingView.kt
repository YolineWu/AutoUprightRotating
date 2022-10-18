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

open class UprightRotatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    lateinit var targetView: View
        private set
    var animDuration: Long = 3000

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.i("yoline", "onLayout")
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
        targetView.animate().apply {
            duration = animDuration
            if (rotation.is90) withStartAction { playTargetSizeAnim() }
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

    private fun playTargetSizeAnim() {
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
            duration = animDuration
            start()
        }
    }
}