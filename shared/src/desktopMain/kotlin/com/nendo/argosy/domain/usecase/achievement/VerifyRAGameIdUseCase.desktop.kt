package com.nendo.argosy.domain.usecase.achievement

import javax.inject.Inject

actual class VerifyRAGameIdUseCase @Inject constructor() {
    actual operator suspend fun invoke(gameId: Long, forceRehash: Boolean): Long? = null
}
