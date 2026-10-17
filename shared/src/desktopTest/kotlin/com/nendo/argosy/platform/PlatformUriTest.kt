package com.nendo.argosy.platform

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformUriTest {
    @Test
    fun partsOfAWebUri() {
        val uri = PlatformUris.parse("https://romm.local:8093/api/roms/12/content/My%20Game.zip?a=1&b=two%20words&flag")
        assertEquals("https", uri.scheme)
        assertEquals("romm.local:8093", uri.authority)
        assertEquals("romm.local", uri.host)
        assertEquals("/api/roms/12/content/My Game.zip", uri.path)
        assertEquals("/api/roms/12/content/My%20Game.zip", uri.encodedPath)
        assertEquals("My Game.zip", uri.lastPathSegment)
        assertEquals(listOf("api", "roms", "12", "content", "My Game.zip"), uri.pathSegments)
        assertEquals(setOf("a", "b", "flag"), uri.queryParameterNames)
        assertEquals("two words", uri.getQueryParameter("b"))
        assertEquals("", uri.getQueryParameter("flag"))
        assertNull(uri.getQueryParameter("missing"))
    }

    @Test
    fun fileUrisUseThreeSlashes() {
        assertEquals("file:///tmp/a%20b.bin", PlatformUris.fromFile(File("/tmp/a b.bin")).toString())
    }

    @Test
    fun encodeMatchesAndroidForSpacesAndPlus() {
        assertEquals("a%20b%2Bc", PlatformUris.encode("a b+c"))
        assertEquals("a b+c", PlatformUris.decode("a%20b%2Bc"))
        assertEquals("a+b", PlatformUris.decode("a+b"))
    }

    @Test
    fun encodeKeepsAndroidsUnreservedSetAndEscapesTheRest() {
        assertEquals("a_b-c!d.e~f'g(h)i*j", PlatformUris.encode("a_b-c!d.e~f'g(h)i*j"))
        assertEquals("a%2Fb%3Fc%3Dd%26e", PlatformUris.encode("a/b?c=d&e"))
        assertEquals("%C3%A9t%C3%A9", PlatformUris.encode("été"))
        assertEquals("été", PlatformUris.decode("%C3%A9t%C3%A9"))
    }

    @Test
    fun opaqueSchemesKeepTheirScheme() {
        assertEquals("steam", PlatformUris.parse("steam://run/570").scheme)
        assertEquals("mailto", PlatformUris.parse("mailto:a@b.c").scheme)
    }
}
