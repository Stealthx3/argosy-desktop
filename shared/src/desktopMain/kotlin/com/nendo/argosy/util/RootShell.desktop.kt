package com.nendo.argosy.util
import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources



actual object RootShell {
    actual val isAvailable: Boolean = false

    actual fun quote(value: String): String = ""

    actual data class Result(actual val exitCode: Int?, actual val output: List<String>) {
        actual val succeeded: Boolean get() = exitCode == 0
    }

    actual fun execute(command: String): kotlin.Result<String?> = kotlin.Result.failure(UnsupportedOperationException("not available on desktop"))

    actual fun run(context: Context, body: String): Result? = null
}
