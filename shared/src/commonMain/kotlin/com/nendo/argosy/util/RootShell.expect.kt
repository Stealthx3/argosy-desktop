package com.nendo.argosy.util

expect object RootShell {
    val isAvailable: Boolean

    fun quote(value: String): String
}
