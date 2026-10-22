package com.nendo.argosy.libretro.touch

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun computeFacePositions(shape: FaceShape, count: Int, btnSize: Dp, gap: Dp): List<DpOffset> {
    val s = btnSize.value
    val g = gap.value
    return when (shape) {
        FaceShape.Single -> listOf(DpOffset(0f, 0f))
        FaceShape.HorizontalPair -> List(count) { i -> DpOffset((s + g) * i, 0f) }
        FaceShape.HorizontalTrio -> List(count) { i -> DpOffset((s + g) * i, 0f) }
        FaceShape.Row4 -> List(count) { i -> DpOffset((s + g) * i, 0f) }
        FaceShape.Row6 -> List(count) { i -> DpOffset((s + g) * (i % 6), 0f) }
        FaceShape.Diamond4 -> listOf(
            DpOffset(0f, s + g),
            DpOffset(s + g, 0f),
            DpOffset(s + g, (s + g) * 2),
            DpOffset((s + g) * 2, s + g)
        ).take(count)
        FaceShape.Stack2x3 -> List(count) { i ->
            val row = i / 3
            val col = i % 3
            DpOffset((s + g) * col, (s + g) * row)
        }
        FaceShape.NbuttonCluster -> List(count) { i -> DpOffset((s + g) * i, 0f) }
    }
}

private data class DpOffset(val x: Float, val y: Float)

internal data class SlotLayout(
    val key: String,
    val offsetX: Dp,
    val offsetY: Dp,
    val widthDp: Dp,
    val heightDp: Dp
)

internal fun layoutFaceSlots(
    spec: TouchLayoutSpec,
    placement: GroupPlacement,
    overrides: Map<String, GroupPlacement>,
    areaW: Dp,
    areaH: Dp,
    sizeScale: Float
): List<SlotLayout> {
    val btnSize = 50.dp * sizeScale * placement.scale
    val gap = 6.dp * sizeScale * placement.scale
    val positions = computeFacePositions(spec.face, spec.faceSlots.size, btnSize, gap)
    val maxX: Float = positions.fold(0f) { acc, p -> kotlin.math.max(acc, p.x + btnSize.value) }
    val maxY: Float = positions.fold(0f) { acc, p -> kotlin.math.max(acc, p.y + btnSize.value) }
    val groupW = maxX.dp
    val groupH = maxY.dp
    val edge = 8.dp
    val originX = (areaW * placement.anchorX - groupW / 2)
        .coerceIn(edge, (areaW - groupW - edge).coerceAtLeast(edge))
    val originY = (areaH * placement.anchorY - groupH / 2)
        .coerceIn(edge, (areaH - groupH - edge).coerceAtLeast(edge))
    return spec.faceSlots.mapIndexed { i, _ ->
        val key = "face_$i"
        val override = overrides[key]
        if (override != null) {
            val w = btnSize * override.scale
            val h = btnSize * override.scale
            val ox = (areaW * override.anchorX - w / 2)
                .coerceIn(edge, (areaW - w - edge).coerceAtLeast(edge))
            val oy = (areaH * override.anchorY - h / 2)
                .coerceIn(edge, (areaH - h - edge).coerceAtLeast(edge))
            SlotLayout(key, ox, oy, w, h)
        } else {
            val pos = positions[i]
            SlotLayout(key, originX + pos.x.dp, originY + pos.y.dp, btnSize, btnSize)
        }
    }
}

internal fun layoutShoulderSlots(
    spec: TouchLayoutSpec,
    placement: GroupPlacement,
    overrides: Map<String, GroupPlacement>,
    areaW: Dp,
    areaH: Dp,
    sizeScale: Float
): List<SlotLayout> {
    val btnSize = 48.dp * sizeScale * placement.scale
    val gap = 8.dp * sizeScale * placement.scale
    val padding = 24.dp
    val centerY = areaH * placement.anchorY
    val edge = 8.dp
    val rawPositions: List<Pair<Dp, Dp>> = when (spec.shoulders) {
        ShoulderShape.None -> emptyList()
        ShoulderShape.TopPair -> listOf(
            padding to centerY - btnSize / 2,
            areaW - padding - btnSize to centerY - btnSize / 2
        )
        ShoulderShape.FourCorners -> listOf(
            padding + btnSize + gap to centerY - btnSize / 2,
            padding to centerY - btnSize / 2,
            areaW - padding - btnSize - btnSize - gap to centerY - btnSize / 2,
            areaW - padding - btnSize to centerY - btnSize / 2
        )
        ShoulderShape.TopPairPlusZ -> listOf(
            padding to centerY - btnSize / 2,
            areaW - padding - btnSize to centerY - btnSize / 2,
            areaW - padding - btnSize to centerY + btnSize / 2 + gap
        )
    }
    return spec.shoulderSlots.mapIndexedNotNull { i, _ ->
        val key = "shoulder_$i"
        val raw = rawPositions.getOrNull(i) ?: return@mapIndexedNotNull null
        val override = overrides[key]
        if (override != null) {
            val w = btnSize * override.scale
            val h = btnSize * override.scale
            val ox = (areaW * override.anchorX - w / 2)
                .coerceIn(edge, (areaW - w - edge).coerceAtLeast(edge))
            val oy = (areaH * override.anchorY - h / 2)
                .coerceIn(edge, (areaH - h - edge).coerceAtLeast(edge))
            SlotLayout(key, ox, oy, w, h)
        } else {
            SlotLayout(key, raw.first, raw.second, btnSize, btnSize)
        }
    }
}

internal fun layoutSystemSlots(
    spec: TouchLayoutSpec,
    placement: GroupPlacement,
    overrides: Map<String, GroupPlacement>,
    areaW: Dp,
    areaH: Dp,
    sizeScale: Float
): List<SlotLayout> {
    if (spec.system.isEmpty()) return emptyList()
    val btnW = 58.dp * sizeScale * placement.scale
    val btnH = 26.dp * sizeScale * placement.scale
    val gap = 10.dp * sizeScale * placement.scale
    val groupW = btnW * spec.system.size + gap * (spec.system.size - 1)
    val edge = 8.dp
    val originX = (areaW * placement.anchorX - groupW / 2)
        .coerceIn(edge, (areaW - groupW - edge).coerceAtLeast(edge))
    val originY = (areaH * placement.anchorY - btnH / 2)
        .coerceIn(edge, (areaH - btnH - edge).coerceAtLeast(edge))
    return spec.system.mapIndexed { i, _ ->
        val key = "system_$i"
        val override = overrides[key]
        if (override != null) {
            val w = btnW * override.scale
            val h = btnH * override.scale
            val ox = (areaW * override.anchorX - w / 2)
                .coerceIn(edge, (areaW - w - edge).coerceAtLeast(edge))
            val oy = (areaH * override.anchorY - h / 2)
                .coerceIn(edge, (areaH - h - edge).coerceAtLeast(edge))
            SlotLayout(key, ox, oy, w, h)
        } else {
            SlotLayout(key, originX + (btnW + gap) * i, originY, btnW, btnH)
        }
    }
}
