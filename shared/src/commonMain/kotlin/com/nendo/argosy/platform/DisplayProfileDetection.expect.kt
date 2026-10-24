package com.nendo.argosy.platform

import com.nendo.argosy.data.preferences.DisplayProfile

/**
 * The display profile this machine looks like, or null where Argosy has no display profiles.
 */
expect object DisplayProfileDetection {
    fun detect(): DisplayProfile?
}
