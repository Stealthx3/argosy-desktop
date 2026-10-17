package com.nendo.argosy.platform

import android.view.KeyEvent

actual fun keyCodeToString(keyCode: Int): String = KeyEvent.keyCodeToString(keyCode)
