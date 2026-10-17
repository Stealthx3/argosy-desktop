package com.nendo.argosy.data.netplay

expect interface LibretroNetplayOps {
    fun setInputPortState(port: Int, bitmask: Int)

    fun getInputPortBitmask(port: Int): Int

    fun renderFrameOnly()
}
