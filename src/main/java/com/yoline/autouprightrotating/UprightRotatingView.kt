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

    private var childView: View? = null
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
    private val isRotating: Boolean get() = rotationRotatingTo != null

    private var childInitLayoutWidth: Int? = null
    private var childInitLayoutHeight: Int? = null

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
        logger.info("UprightRotatingView>>>onLayout isRotating=$isRotating")
        require(childCount == 1) { "There can be exactly one child view，there are currently $childCount" }
        if (firstOnLayout) {
            childView = children.first().also { child ->
                lastRotation =
                    child.pivotRotation(0f) ?: throw IllegalStateException("子View的rotation必须是90的倍数")
                childInitLayoutHeight = child.layoutParams.height
                childInitLayoutWidth = child.layoutParams.width
            }
            firstOnLayout = false
        }
        childView!!.let { child ->
            logger.info("UprightRotatingView>>>onLayout child.height=$lastHeight, width=$lastWidth, rotation=${child.rotation}")
            child.pivotX = child.width / 2f
            child.pivotY = child.height / 2f
            require((child.layoutParams as LayoutParams).gravity == Gravity.CENTER) { "子View的layout_gravity必须是center" }
            if (!isRotating) {
                lastHeight = child.height
                lastWidth = child.width
                child.pivotRotation(0f)?.let { lastRotation = it }
                fixChildWidthHeight()
            }
        }
    }

    fun childRotationBy(@UprightDegrees degrees: Int, onEnd: ((destRotation: UprightRotation) -> Unit)? = null) {
        UprightRotation.byDegrees(degrees)?.let {
            childRotationBy(it, onEnd)
        } ?: throw IllegalArgumentException("degrees 只能是-90、0、90或180，当前为$degrees")
    }

    fun childRotationBy(rotation: UprightRotation, onEnd: ((destRotation: UprightRotation) -> Unit)? = null) {
        childView?.let {
            val fromRotation = rotationRotatingTo ?: it.pivotRotation(0f)
            require(fromRotation != null) { "调用此方法时子View的rotation必须是90的倍数" }
            childRotationTo(fromRotation.plus(rotation), onEnd)
        } ?: throwNotLayout()
    }

    fun childRotationTo(@UprightDegrees degrees: Int, onEnd: ((destRotation: UprightRotation) -> Unit)? = null) {
        UprightRotation.byDegrees(degrees)?.let {
            childRotationTo(it, onEnd)
        } ?: throw IllegalArgumentException("degrees 只能是-90、0、90或180，当前为$degrees")
    }

    fun childRotationTo(rotation: UprightRotation, onEnd: ((destRotation: UprightRotation) -> Unit)? = null) {
        childView?.let { childView ->
            childView.animate().cancel()
            rotationRotatingTo = rotation
            if (rotatingDuration == 0L) {
                if (rotation.minus(lastRotation).is90) setTargetWidthHeight(
                    childView.height, childView.width, 0
                )
                childView.rotation = rotation.degrees.toFloat()
                onRotationEnd(onEnd)
                return
            }
            childView.animate().apply {
                val byDegrees = (rotation.degrees - childView.rotation).degreesShrink()
                logger.info("childRotationTo byDegrees=$byDegrees")
                duration = (durationPerDegrees * abs(byDegrees)).roundToLong()
                if (rotation.minus(lastRotation).is90) withStartAction {
                    setTargetWidthHeight(lastHeight, lastWidth, duration)
                }
                withEndAction { onRotationEnd(onEnd) }
                rotationBy(byDegrees)
            }
        } ?: throwNotLayout()
    }

    private fun fixChildWidthHeight() {
        childView!!.let { child ->
            logger.info("UprightRotatingView>>>fixChildWidthHeight")
            logger.info("UprightRotatingView>>>fixChildWidthHeight child.height=$lastHeight, width=$lastWidth, rotation=${child.rotation}")
            child.layoutParams.let {
                val heightToSet = if (lastRotation.is90) this.width else this.height
                val changeHeight = it.height != heightToSet && childInitLayoutHeight == LayoutParams.MATCH_PARENT
                val widthToSet = if (lastRotation.is90) this.height else this.width
                val changeWidth = it.width != widthToSet && childInitLayoutWidth == LayoutParams.MATCH_PARENT
                if (changeHeight || changeWidth) {
                    child.layoutParams = it.also {
                        if (changeHeight) it.height = heightToSet
                        if (changeWidth) it.width = widthToSet
                    }
                }
            }
        }
    }

    private fun onRotationEnd(onEnd: ((destRotation: UprightRotation) -> Unit)? = null) {
        childView!!.let {
            lastHeight = it.height
            lastWidth = it.width
            lastRotation = it.pivotRotationNotNull(0)
            rotationRotatingTo = null
            logger.info("animate end childView.rotation=${it.rotation}")
            fixChildWidthHeight()
            onEnd?.let { it(lastRotation) }
        }
    }

    private fun setTargetWidthHeight(toWidth: Int, toHeight: Int, rotatingDuration: Long) {
        logger.info("UprightRotatingView>>>toWidth=$toWidth, toHeight=$toHeight, duration=$rotatingDuration")
        childView?.let { childView ->
            if (rotatingDuration == 0L) {
                childView.layoutParams = childView.layoutParams.apply {
                    height = toHeight
                    width = toWidth
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
        } ?: throwNotLayout()
    }

    private fun throwNotLayout() {
        throw IllegalStateException("UprightRotatingView is not layout, call after onLayout")
    }

    companion object {
        const val DEFAULT_ROTATING_DURATION = 300L
    }
}