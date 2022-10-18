package com.yoline.autouprightrotating

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.children
import com.yoline.autouprightrotating.rotation.UprightDegrees
import com.yoline.autouprightrotating.rotation.UprightRotation
import com.yoline.autouprightrotating.rotation.UprightRotation.Companion.pivotRotation
import com.yoline.autouprightrotating.rotation.UprightRotation.Companion.pivotRotationNotNull
import com.yoline.autouprightrotating.utils.DegreesUtils.degreesShrink
import java.util.logging.Logger
import kotlin.math.abs
import kotlin.math.roundToLong

class UprightRotatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val logger: Logger = Logger.getLogger("yoline")

    lateinit var childView: View
        private set
    var rotatingDuration: Long = DEFAULT_ROTATING_DURATION
    var durationPerDegrees: Long
        get() = (rotatingDuration / 90f).roundToLong()
        set(value) {
            rotatingDuration = value * 90
        }
    private var firstOnLayout = true
    private var rotationRotatingTo: UprightRotation? = null
    private var lastRotation: UprightRotation = UprightRotation.ROTATION_0
    private var lastHeight: Int = 0
    private var lastWidth: Int = 0

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
        childView = children.first().apply {
            pivotX = width / 2f
            pivotY = height / 2f
        }
        require((childView.layoutParams as LayoutParams).gravity == Gravity.CENTER) { "子View的layout_gravity必须是center" }
        if (firstOnLayout) {
            init()
            firstOnLayout = false
        }
    }

    private fun init() {
        lastHeight = childView.height
        lastWidth = childView.width
        lastRotation =
            childView.pivotRotation(0) ?: throw IllegalStateException("子View的rotation必须是90的倍数")
    }

    fun childRotationBy(@UprightDegrees degrees: Int) {
        UprightRotation.byDegrees(degrees)?.let {
            childRotationBy(it)
        } ?: throw IllegalArgumentException("degrees 只能是-90、0、90或180，当前为$degrees")
    }

    fun childRotationBy(rotation: UprightRotation) {
        val fromRotation = rotationRotatingTo ?: childView.pivotRotation(0)
        require(fromRotation != null) { "调用此方法时子View的rotation必须是90的倍数" }
        childRotationTo(fromRotation.plus(rotation))
    }

    fun childRotationTo(@UprightDegrees degrees: Int) {
        UprightRotation.byDegrees(degrees)?.let {
            childRotationTo(it)
        } ?: throw IllegalArgumentException("degrees 只能是-90、0、90或180，当前为$degrees")
    }

    fun childRotationTo(rotation: UprightRotation) {
        childView.animate().cancel()
        if (rotatingDuration == 0L) {
            if (rotation.minus(lastRotation).is90) setTargetWidthHeight(
                childView.height, childView.width, 0
            )
            childView.rotation = rotation.degrees.toFloat()
            return
        }
        childView.animate().apply {
            val byDegrees = (rotation.degrees - childView.rotation).degreesShrink()
            logger.info("childRotationTo byDegrees=$byDegrees")
            duration = (durationPerDegrees * abs(byDegrees)).roundToLong()
            if (rotation.minus(lastRotation).is90) withStartAction {
                setTargetWidthHeight(lastHeight, lastWidth, duration)
            }
            withEndAction {
                lastHeight = childView.height
                lastWidth = childView.width
                lastRotation = childView.pivotRotationNotNull(0)
                rotationRotatingTo = null
                logger.info("animate end childView.rotation=${childView.rotation}")
            }
            rotationRotatingTo = rotation
            rotationBy(byDegrees)
        }
    }

    private fun setTargetWidthHeight(toWidth: Int, toHeight: Int, rotatingDuration: Long) {
        if (rotatingDuration == 0L) {
            childView.layoutParams = childView.layoutParams.apply {
                height = toWidth
                width = toHeight
            }
            return
        }
        val heightAnim = ValueAnimator.ofInt(childView.height, toHeight).apply {
            addUpdateListener {
                childView.layoutParams = childView.layoutParams.apply {
                    height = it.animatedValue as Int
                }
            }
        }
        val widthAnim = ValueAnimator.ofInt(childView.width, toWidth).apply {
            addUpdateListener {
                childView.layoutParams = childView.layoutParams.apply {
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