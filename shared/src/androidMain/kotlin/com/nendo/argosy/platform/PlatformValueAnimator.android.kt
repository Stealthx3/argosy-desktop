package com.nendo.argosy.platform

import android.animation.ValueAnimator

actual object PlatformValueAnimator {
    actual fun areAnimatorsEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
}
