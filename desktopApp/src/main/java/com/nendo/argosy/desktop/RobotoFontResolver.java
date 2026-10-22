package com.nendo.argosy.desktop;

import androidx.compose.ui.text.font.FontFamily;
import androidx.compose.ui.text.font.FontFamilyResolverImpl;
import androidx.compose.ui.text.font.FontFamilyResolverKt;
import androidx.compose.ui.text.font.FontListFontFamilyTypefaceAdapter;
import androidx.compose.ui.text.font.PlatformFontFamilyTypefaceAdapter;
import androidx.compose.ui.text.font.PlatformResolveInterceptor;
import androidx.compose.ui.text.font.SkiaFontLoader;
import androidx.compose.ui.text.platform.FontCache;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Compose's own resolver, built the way createFontFamilyResolver() builds it, with an interceptor
 * that answers Default and SansSerif with Roboto as Android does. Written in Java because the
 * interceptor and the resolver class are Kotlin-internal, and Compose's paragraph code casts the
 * resolver to FontFamilyResolverImpl, so a wrapping resolver would not survive layout.
 */
final class RobotoFontResolver {
    private RobotoFontResolver() {
    }

    static FontFamily.Resolver create(FontFamily roboto) {
        PlatformResolveInterceptor interceptor = new PlatformResolveInterceptor() {
            @Override
            public FontFamily interceptFontFamily(FontFamily fontFamily) {
                if (fontFamily == null || fontFamily == FontFamily.Companion.getDefault()
                        || fontFamily == FontFamily.Companion.getSansSerif()) {
                    return roboto;
                }
                return fontFamily;
            }
        };
        return new FontFamilyResolverImpl(
                new SkiaFontLoader(new FontCache()),
                interceptor,
                FontFamilyResolverKt.getGlobalTypefaceRequestCache(),
                new FontListFontFamilyTypefaceAdapter(
                        FontFamilyResolverKt.getGlobalAsyncTypefaceCache(),
                        EmptyCoroutineContext.INSTANCE),
                new PlatformFontFamilyTypefaceAdapter());
    }
}
