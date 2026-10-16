package spike

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import java.io.File
import javax.swing.SwingUtilities
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * Headless render: composes LibraryPreview off-screen and writes the frame to the path in args[0].
 * Every scene call runs on the AWT thread, which is Dispatchers.Main here (kotlinx-coroutines-swing),
 * so Compose's own posted callbacks and the frames never interleave.
 */
fun main(args: Array<String>) {
    seedLibrary()
    val out = File(args.firstOrNull() ?: "library.png")
    lateinit var scene: ImageComposeScene
    lateinit var image: Image
    SwingUtilities.invokeAndWait {
        scene = ImageComposeScene(width = 1280, height = 800, density = Density(1f)) { LibraryPreview() }
    }
    // Covers decode on Dispatchers.IO (as Coil does); let frames pass until they have landed.
    for (frame in 0..40) {
        SwingUtilities.invokeAndWait { image = scene.render(frame * 50_000_000L) }
        Thread.sleep(50)
    }
    SwingUtilities.invokeAndWait { scene.close() }
    out.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    println("wrote ${out.absolutePath}")
}
