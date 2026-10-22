package com.nendo.argosy.ui.components

import com.nendo.argosy.platform.PlatformUri as Uri
import com.nendo.argosy.platform.authority
import com.nendo.argosy.platform.encodedPath
import com.nendo.argosy.platform.getQueryParameter
import com.nendo.argosy.platform.host
import com.nendo.argosy.platform.lastPathSegment
import com.nendo.argosy.platform.path
import com.nendo.argosy.platform.pathSegments
import com.nendo.argosy.platform.queryParameterNames
import com.nendo.argosy.platform.scheme
import com.nendo.argosy.platform.PlatformUris
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class PairingQrResult(
    val origin: String,
    val code: String
)

fun parsePairingQrUrl(url: String): PairingQrResult? {
    return try {
        val uri = PlatformUris.parse(url)
        val code = uri.getQueryParameter("code") ?: return null
        val origin = "${uri.scheme}://${uri.authority}"
        if (origin.isBlank() || code.isBlank()) return null
        PairingQrResult(origin, code.replace("-", "").uppercase())
    } catch (_: Exception) {
        null
    }
}
