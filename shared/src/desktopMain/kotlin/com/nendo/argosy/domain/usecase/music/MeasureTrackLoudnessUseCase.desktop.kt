package com.nendo.argosy.domain.usecase.music

import javax.inject.Inject

actual class MeasureTrackLoudnessUseCase @Inject constructor() {
    actual operator suspend fun invoke(filePath: String): Double? = null
}
