package com.nendo.argosy.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformHtmlTest {
    @Test
    fun tagsAreStrippedAndEntitiesDecoded() {
        assertEquals("Infinite lives & 99 coins", PlatformHtml.fromHtml("<b>Infinite</b> lives &amp; 99 coins", 0).toString())
        assertEquals("a\nb", PlatformHtml.fromHtml("a<br>b", 0).toString())
        assertEquals("one\n\ntwo", PlatformHtml.fromHtml("<p>one</p><p>two</p>", 0).toString())
        assertEquals("é <x>", PlatformHtml.fromHtml("&#233; &lt;x&gt;", 0).toString())
    }
}
