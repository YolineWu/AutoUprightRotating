package com.yoline.autouprightrotating.rotation

import android.view.Surface
import androidx.annotation.IntDef

@IntDef(flag = true, value = [Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270])
@Retention(AnnotationRetention.SOURCE)
annotation class SurfaceRotation
