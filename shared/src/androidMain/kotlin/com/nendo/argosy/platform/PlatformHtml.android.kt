package com.nendo.argosy.platform

import android.text.Html

actual object PlatformHtml {
    actual val FROM_HTML_MODE_LEGACY: Int get() = Html.FROM_HTML_MODE_LEGACY

    actual fun fromHtml(source: String, flags: Int): CharSequence = Html.fromHtml(source, flags) ?: ""
}
