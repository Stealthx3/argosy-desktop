package com.nendo.argosy.platform

import com.nendo.argosy.data.preferences.DisplayProfile

/**
 * The display profile this machine looks like, or null where Argosy has no display profiles, and
 * whether Argosy runs under Steam Game Mode.
 */
expect object DisplayProfileDetection {
    fun detect(): DisplayProfile?

    fun inGameMode(): Boolean
}
