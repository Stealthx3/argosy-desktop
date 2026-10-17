package com.nendo.argosy.platform

import com.nendo.argosy.core.input.AndroidKeyCodes
import java.lang.reflect.Modifier

private val keyNames: Map<Int, String> by lazy {
    AndroidKeyCodes::class.java.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType && it.name.startsWith("KEYCODE_") }
        .associate { it.getInt(null) to it.name }
}

actual fun keyCodeToString(keyCode: Int): String = keyNames[keyCode] ?: keyCode.toString()
