package com.yoline.autouprightrotating.rotation

import androidx.annotation.IntDef

@IntDef(flag = true, value = [0, 90, 180, -90])
@Retention(AnnotationRetention.SOURCE)
annotation class UprightDegrees
