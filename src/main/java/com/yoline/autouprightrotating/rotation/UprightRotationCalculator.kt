package com.yoline.autouprightrotating.rotation

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.OrientationEventListener
import android.view.View

open class UprightRotationCalculator<view : View>(
    val context: Context,
    val rotateView: view,
    val maxOffsetDegrees: Float = DEFAULT_MAX_OFFSET,
    var onViewRotate: ((rotateView: view, uprightRotation: UprightRotation) -> Unit)? = DEFAULT_ON_VIEW_ROTATE
) {
    open var displayRotation: UprightRotation = UprightRotation.ROTATION_0
        set(value) {
            Log.i("yoline", "rotation displayRotation=$value")
            field.let {
                field = value
                if (it != value) onRotationUpdate()
            }
        }

    open var deviceRotation: UprightRotation = UprightRotation.ROTATION_0
        set(value) {
            Log.i("yoline", "rotation deviceRotation=$value")
            field.let {
                field = value
                if (it != value) onRotationUpdate()
            }
        }

    @SuppressLint("WrongConstant")
    open fun start() {
        rotateView.display?.let {
            displayRotation = UprightRotation.bySurfaceRotation(it.rotation)
        }
        this.registerConfigChangeReceiver()
        orientationEvent.enable()
    }

    open fun stop() {
        try {
            context.unregisterReceiver(mConfigChangeReceiver)
        } catch (_: IllegalArgumentException) {
        }
        orientationEvent.disable()
    }

    protected open fun onRotationUpdate() {
        Log.i(
            "yoline",
            "rotation onRotationUpdate deviceRotation=$deviceRotation displayRotation=$displayRotation"
        )
        onViewRotate?.let { it(rotateView, deviceRotation.reverse().minus(displayRotation)) }
    }

    private val orientationEvent = object : OrientationEventListener(context) {
        override fun onOrientationChanged(degrees: Int) {
            if (degrees == ORIENTATION_UNKNOWN) return
            UprightRotation.byAnyDegrees(degrees, maxOffsetDegrees)?.let {
                if (deviceRotation == it) return else deviceRotation = it
            }
        }
    }

    /**
     * 系统配置信息改变广播接收者，主要用于监听屏幕方向的改变
     */
    private val mConfigChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("WrongConstant")
        override fun onReceive(context: Context, intent: Intent) {
            rotateView.display?.rotation?.let {
                displayRotation = UprightRotation.bySurfaceRotation(it)
            }
        }
    }

    /**
     * 注册系统配置信息改变广播接收者
     */
    private fun registerConfigChangeReceiver() {
        val configChangeFilter = IntentFilter()
        configChangeFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        context.registerReceiver(mConfigChangeReceiver, configChangeFilter)
    }

    companion object {
        const val DEFAULT_MAX_OFFSET = 15f

        fun <view : View> view.autoRotate(
            context: Context,
            maxOffsetDegrees: Float = DEFAULT_MAX_OFFSET,
            onViewRotate: ((rotatingView: view, rotation: UprightRotation) -> Unit)? = DEFAULT_ON_VIEW_ROTATE
        ): UprightRotationCalculator<view> {
            return UprightRotationCalculator(
                context, this, maxOffsetDegrees, onViewRotate
            ).apply { start() }
        }

        val DEFAULT_ON_VIEW_ROTATE = fun(rotatingView: View, rotation: UprightRotation) {
            rotatingView.animate().rotation(rotation.degrees.toFloat())
        }
    }
}