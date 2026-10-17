package com.nendo.argosy.platform

actual open class PlatformApplication(
    private val base: PlatformContext = DesktopPlatformContext(),
) : PlatformContext() {
    override val platformResources: PlatformResources get() = base.platformResources
    override val platformFilesDir: java.io.File get() = base.platformFilesDir
    override val platformCacheDir: java.io.File get() = base.platformCacheDir
    override val platformAssets: PlatformAssets get() = base.platformAssets
}
