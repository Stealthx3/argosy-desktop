package com.nendo.argosy.data.quaypass

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class ClientFingerprint @Inject constructor() {
    actual val apkSigningCertHash: String = ""
}
