package com.nendo.argosy.util



actual object RootShell {
    actual val isAvailable: Boolean = false

    actual fun quote(value: String): String = ""
}
