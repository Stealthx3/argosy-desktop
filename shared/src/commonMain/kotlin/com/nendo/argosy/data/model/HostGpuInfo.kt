package com.nendo.argosy.data.model

enum class HostGpuType { DISCRETE, INTEGRATED, VIRTUAL, OTHER }

data class HostGpu(
    val name: String,
    val vendor: String?,
    val type: HostGpuType,
    val kernelDriver: String?,
    val driver: String?,
    val vramMb: Int?,
    val vulkanVersion: String?,
    val openGlVersion: String?
)

data class HostDisplay(
    val connector: String,
    val width: Int?,
    val height: Int?,
    val diagonalInches: Double?
)

data class HostGpuInfo(
    val gpus: List<HostGpu> = emptyList(),
    val displays: List<HostDisplay> = emptyList()
)
