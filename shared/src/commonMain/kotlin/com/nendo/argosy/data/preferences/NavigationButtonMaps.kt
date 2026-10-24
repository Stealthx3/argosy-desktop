package com.nendo.argosy.data.preferences

/**
 * Per-controller navigation remaps. Each map is a permutation of key codes, stored as its
 * non-identity entries: a pressed key code becomes the key code the input handler sees, before
 * the A/B, X/Y and Start/Select swaps apply. Controllers are keyed by SDL GUID, the keyboard by
 * [KEYBOARD].
 */
data class NavigationButtonMaps(val byController: Map<String, Map<Int, Int>> = emptyMap()) {

    fun translate(controller: String, keyCode: Int): Int =
        byController[controller]?.get(keyCode) ?: keyCode

    fun physicalFor(controller: String, mappedKeyCode: Int): Int =
        byController[controller]?.entries?.firstOrNull { it.value == mappedKeyCode }?.key ?: mappedKeyCode

    fun assign(controller: String, physicalKeyCode: Int, mappedKeyCode: Int): NavigationButtonMaps {
        val current = byController[controller].orEmpty()
        val displaced = physicalFor(controller, mappedKeyCode)
        val previous = translate(controller, physicalKeyCode)
        val next = current.toMutableMap().apply {
            put(physicalKeyCode, mappedKeyCode)
            put(displaced, previous)
            entries.removeAll { it.key == it.value }
        }
        return withController(controller, next)
    }

    fun reset(controller: String): NavigationButtonMaps = withController(controller, emptyMap())

    fun encode(): String = byController.entries
        .filter { it.value.isNotEmpty() }
        .joinToString(CONTROLLER_SEPARATOR) { (controller, map) ->
            controller + CONTROLLER_KEY_SEPARATOR +
                map.entries.joinToString(ENTRY_SEPARATOR) { "${it.key}$PAIR_SEPARATOR${it.value}" }
        }

    private fun withController(controller: String, map: Map<Int, Int>): NavigationButtonMaps =
        NavigationButtonMaps(if (map.isEmpty()) byController - controller else byController + (controller to map))

    companion object {
        const val KEYBOARD = "keyboard"
        private const val CONTROLLER_SEPARATOR = ";"
        private const val CONTROLLER_KEY_SEPARATOR = "="
        private const val ENTRY_SEPARATOR = ","
        private const val PAIR_SEPARATOR = ":"

        fun decode(raw: String?): NavigationButtonMaps {
            if (raw.isNullOrBlank()) return NavigationButtonMaps()
            val controllers = raw.split(CONTROLLER_SEPARATOR).mapNotNull { entry ->
                val parts = entry.split(CONTROLLER_KEY_SEPARATOR, limit = 2)
                if (parts.size != 2 || parts[0].isBlank()) return@mapNotNull null
                val pairs = parts[1].split(ENTRY_SEPARATOR).mapNotNull { pair ->
                    val codes = pair.split(PAIR_SEPARATOR)
                    val from = codes.getOrNull(0)?.toIntOrNull()
                    val to = codes.getOrNull(1)?.toIntOrNull()
                    if (codes.size == 2 && from != null && to != null && from != to) from to to else null
                }.toMap()
                if (isPermutation(pairs)) parts[0] to pairs else null
            }.toMap()
            return NavigationButtonMaps(controllers)
        }

        private fun isPermutation(map: Map<Int, Int>): Boolean =
            map.isNotEmpty() && map.keys == map.values.toSet()
    }
}
