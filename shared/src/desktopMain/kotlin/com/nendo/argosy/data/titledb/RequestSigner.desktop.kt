package com.nendo.argosy.data.titledb

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class RequestSigner @Inject constructor() {
    actual fun isConfigured(): Boolean = false
}
