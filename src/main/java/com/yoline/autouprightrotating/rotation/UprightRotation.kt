package com.yoline.autouprightrotating.rotation

import android.view.Surface
import android.view.View
import com.yoline.autouprightrotating.utils.DegreesUtils.degreesShrink
import kotlin.math.roundToInt

/**
 * 表示悬浮窗的内容相对于其轴心旋转的度数或方向
 */
enum class UprightRotation(@UprightDegrees val degrees: Int, @SurfaceRotation val rotation: Int) {

    /**
     * 表示相对于轴心0度方向或旋转0度，或相当于[Surface.ROTATION_0]
     * @see View.getRotation
     */
    ROTATION_0(0, Surface.ROTATION_0),

    /**
     * 表示相对于轴心顺时针90度方向或顺时针旋转90度，或相当于[Surface.ROTATION_90]
     * @see View.getRotation
     */
    ROTATION_CW_90(90, Surface.ROTATION_90),

    /**
     * 表示相对于轴心180度方向或旋转180度，或相当于[Surface.ROTATION_180]
     * @see View.getRotation
     */
    ROTATION_180(180, Surface.ROTATION_180),

    /**
     * 表示相对于轴心逆时针90度方向或逆时针旋转90度，或相当于[Surface.ROTATION_270]
     * @see View.getRotation
     */
    ROTATION_CCW_90(-90, Surface.ROTATION_270);

    fun plus(uprightRotation: UprightRotation): UprightRotation {
        return values().first { it.degrees == (this.degrees + uprightRotation.degrees).degreesShrink() }
    }

    fun minus(uprightRotation: UprightRotation): UprightRotation {
        return values().first { it.degrees == (this.degrees - uprightRotation.degrees).degreesShrink() }
    }

    fun reverse(): UprightRotation {
        return when (this) {
            ROTATION_CW_90 -> ROTATION_CCW_90
            ROTATION_CCW_90 -> ROTATION_CW_90
            else -> this
        }
    }

    val is90 get() = this == ROTATION_CW_90 || this == ROTATION_CCW_90

    fun isInLineWith(uprightRotation: UprightRotation): Boolean {
        return is90 == uprightRotation.is90
    }

    companion object {

        fun bySurfaceRotation(@SurfaceRotation rotation: Int): UprightRotation {
            return values().first { it.rotation == rotation }
        }

        fun byDegrees(@UprightDegrees degrees: Int): UprightRotation? {
            return values().firstOrNull { it.degrees == degrees}
        }

        fun byAnyDegrees(degrees: Int, maxOffset: Float): UprightRotation? {
            return degrees.degreesShrink().let {
                if (it >= 0 - maxOffset && it <= 0 + maxOffset) ROTATION_0
                else if (it >= 90 - maxOffset && it <= 90 + maxOffset) ROTATION_CW_90
                else if (it >= -90 - maxOffset && it <= -90 + maxOffset) ROTATION_CCW_90
                else if (it <= -180 + maxOffset || it >= 180 - maxOffset) ROTATION_180
                else null
            }
        }

        fun View.displayRotation(): UprightRotation? {
            return this.display?.rotation?.let { bySurfaceRotation(it) }
        }

        fun View.pivotRotationNotNull(maxOffsetDegrees: Int): UprightRotation {
            return pivotRotation(maxOffsetDegrees.toFloat())!!
        }

        fun View.pivotRotation(maxOffsetDegrees: Float = 45f): UprightRotation? {
            return byAnyDegrees(this.rotation.roundToInt(), maxOffsetDegrees)
        }

        fun View.pivotRotationNotNull(maxOffsetDegrees: Float = 45f): UprightRotation {
            return pivotRotation(maxOffsetDegrees)!!
        }
    }
}
