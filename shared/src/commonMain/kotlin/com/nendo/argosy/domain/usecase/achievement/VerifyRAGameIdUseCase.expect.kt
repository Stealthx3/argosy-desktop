package com.nendo.argosy.domain.usecase.achievement

expect class VerifyRAGameIdUseCase {
    operator suspend fun invoke(gameId: Long, forceRehash: Boolean): Long?
}
