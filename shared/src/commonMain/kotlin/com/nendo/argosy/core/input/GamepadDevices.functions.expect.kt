package com.nendo.argosy.core.input

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice

expect fun InputDevice.actsAsGamepad(): Boolean

expect fun InputDevice.isPhysicalGamepad(): Boolean
