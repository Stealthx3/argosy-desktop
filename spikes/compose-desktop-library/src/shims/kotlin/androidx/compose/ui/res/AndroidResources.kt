package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import org.xml.sax.InputSource
import spike.res.ResourceTable

@Composable
fun stringResource(id: Int): String = ResourceTable.string(id)

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String = ResourceTable.string(id, *formatArgs)

@Composable
fun pluralStringResource(id: Int, count: Int): String = ResourceTable.quantity(id, count)

@Composable
fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String =
    ResourceTable.quantity(id, count, *formatArgs)

/** Loads upstream's Android vector drawable XML (res/drawable, copied onto the classpath) as-is. */
@Composable
fun painterResource(id: Int): Painter {
    val density = LocalDensity.current
    val vector = remember(id, density) {
        val name = ResourceTable.drawableName(id)
        val stream = ResourceTable::class.java.getResourceAsStream("/drawable/$name.xml")
            ?: error("drawable $name is not a vector XML")
        stream.use { loadXmlImageVector(InputSource(it), density) }
    }
    return rememberVectorPainter(vector)
}
