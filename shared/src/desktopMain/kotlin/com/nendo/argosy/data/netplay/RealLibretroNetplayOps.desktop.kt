package com.nendo.argosy.data.netplay



actual object RealLibretroNetplayOps : LibretroNetplayOps {
    override fun setInputPortState(port: Int, bitmask: Int) {
    }

    override fun getInputPortBitmask(port: Int): Int = 0

    override fun renderFrameOnly() {
    }
}
