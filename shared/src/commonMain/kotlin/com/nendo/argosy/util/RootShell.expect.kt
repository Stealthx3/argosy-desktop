package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

expect object RootShell {
    class Result {
        val exitCode: Int?
        val output: List<String>
        val succeeded: Boolean
    }

    val isAvailable: Boolean

    fun execute(command: String): kotlin.Result<String?>

    fun quote(value: String): String

    fun run(context: Context, body: String): Result?
}
