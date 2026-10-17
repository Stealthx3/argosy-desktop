package com.nendo.argosy.data.netplay

import javax.inject.Inject

actual interface LibretroNetplayOps {
    actual fun setInputPortState(port: Int, bitmask: Int)

    actual fun getInputPortBitmask(port: Int): Int

    actual fun renderFrameOnly()
}
