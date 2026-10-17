package com.nendo.argosy.data.quaypass

expect object QuayPassRasterPng {
    fun fromSparseBase64(sparseBase64: String?): String?

    fun fromRasterBytes(rasterBytes: ByteArray?): String?
}
