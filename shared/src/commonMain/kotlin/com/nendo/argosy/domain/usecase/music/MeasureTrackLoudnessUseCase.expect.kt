package com.nendo.argosy.domain.usecase.music

expect class MeasureTrackLoudnessUseCase {
    operator suspend fun invoke(filePath: String): Double?
}
