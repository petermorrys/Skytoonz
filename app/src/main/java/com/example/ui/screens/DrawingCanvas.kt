package com.example.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.model.DrawPathEntity
import com.example.data.model.LayerEntity
import com.example.ui.drawing.BrushType
import com.example.ui.viewmodel.AnimationViewModel
import com.example.util.CanvasPoint
import com.example.util.PathSerializer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    viewModel: AnimationViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val currentFrameIndex by viewModel.currentFrameIndex.collectAsState()
    val frames by viewModel.frames.collectAsState()
    val layers by viewModel.layers.collectAsState()
    val pathsMap by viewModel.pathsMap.collectAsState()
    val currentDrawingPoints by viewModel.currentDrawingPoints.collectAsState()
    val brushConfig by viewModel.brushConfig.collectAsState()
    val onionSkinBefore by viewModel.onionSkinBefore.collectAsState()
    val onionSkinAfter by viewModel.onionSkinAfter.collectAsState()
    val onionSkinOpacity by viewModel.onionSkinOpacity.collectAsState()
    val isPressureEnabled by viewModel.isPressureSensitivityEnabled.collectAsState()
    val pressureMultiplier by viewModel.stylusPressureMultiplier.collectAsState()

    // Ruler states collected from ViewModel
    val rulerEnabled by viewModel.rulerEnabled.collectAsState()
    val selectedRuler by viewModel.selectedRuler.collectAsState()
    val rulerLocked by viewModel.rulerLocked.collectAsState()
    val rulerCenter by viewModel.rulerCenter.collectAsState()
    val rulerRadius by viewModel.rulerRadius.collectAsState()

    // Dragging state local to this canvas touch session
    var rulerDragMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val currentFrameId = viewModel.getCurrentFrameId()

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .pointerInput(currentFrameId, rulerEnabled, selectedRuler, rulerLocked, rulerCenter, rulerRadius) {
                if (currentFrameId == null) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        val event: PointerEvent = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val position = change.position

                        // Get touch / stylus pressure
                        val rawPressure = change.pressure
                        val pressure = if (isPressureEnabled) {
                            rawPressure * pressureMultiplier
                        } else {
                            1.0f
                        }

                        when (event.type) {
                            PointerEventType.Press -> {
                                if (rulerEnabled && !rulerLocked && (selectedRuler == "CIRC" || selectedRuler == "BOX")) {
                                    val cx = rulerCenter.x
                                    val cy = rulerCenter.y
                                    val r = rulerRadius
                                    val distToCenter = Math.sqrt(((position.x - cx) * (position.x - cx) + (position.y - cy) * (position.y - cy)).toDouble()).toFloat()
                                    
                                    val rx = if (selectedRuler == "CIRC") cx + r else cx + r
                                    val ry = if (selectedRuler == "CIRC") cy else cy + r
                                    val distToResize = Math.sqrt(((position.x - rx) * (position.x - rx) + (position.y - ry) * (position.y - ry)).toDouble()).toFloat()

                                    if (distToResize < 80f) {
                                        rulerDragMode = "RESIZE"
                                        change.consume()
                                    } else if (distToCenter < 80f) {
                                        rulerDragMode = "CENTER"
                                        change.consume()
                                    } else {
                                        rulerDragMode = null
                                        change.consume()
                                        viewModel.startDrawingPath(position.x, position.y, pressure)
                                    }
                                } else {
                                    rulerDragMode = null
                                    change.consume()
                                    viewModel.startDrawingPath(position.x, position.y, pressure)
                                }
                            }
                            PointerEventType.Move -> {
                                change.consume()
                                if (rulerDragMode == "CENTER") {
                                    viewModel.setRulerCenter(position.x, position.y)
                                } else if (rulerDragMode == "RESIZE") {
                                    val cx = rulerCenter.x
                                    val cy = rulerCenter.y
                                    val newR = if (selectedRuler == "CIRC") {
                                        Math.sqrt(((position.x - cx) * (position.x - cx) + (position.y - cy) * (position.y - cy)).toDouble()).toFloat()
                                    } else {
                                        maxOf(Math.abs(position.x - cx), Math.abs(position.y - cy))
                                    }
                                    viewModel.setRulerRadius(newR)
                                } else {
                                    viewModel.addPointToDrawingPath(position.x, position.y, pressure)
                                }
                            }
                            PointerEventType.Release -> {
                                change.consume()
                                if (rulerDragMode != null) {
                                    rulerDragMode = null
                                } else {
                                    viewModel.endDrawingPath()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // MANDATORY for BlendMode.Clear (Eraser) to avoid cutting black holes in the UI:
                .graphicsLayer { alpha = 0.99f }
        ) {
            // 1. Draw custom background chosen in project settings
            drawCustomBackground(project?.backgroundType ?: "White")

            if (currentFrameId == null) return@Canvas

            // 2. Draw Onion Skin BEFORE (Previous Frame Paths in translucent Red/Orange)
            if (onionSkinBefore) {
                val beforePaths = viewModel.getOnionSkinBeforePaths()
                beforePaths.forEach { drawPathEntity ->
                    drawVectorPath(
                        drawPathEntity = drawPathEntity,
                        overrideColor = Color(220, 80, 80).copy(alpha = onionSkinOpacity),
                        pressureEnabled = isPressureEnabled,
                        pressureMult = pressureMultiplier
                    )
                }
            }

            // 3. Draw Onion Skin AFTER (Next Frame Paths in translucent Green/Blue)
            if (onionSkinAfter) {
                val afterPaths = viewModel.getOnionSkinAfterPaths()
                afterPaths.forEach { drawPathEntity ->
                    drawVectorPath(
                        drawPathEntity = drawPathEntity,
                        overrideColor = Color(60, 160, 240).copy(alpha = onionSkinOpacity),
                        pressureEnabled = isPressureEnabled,
                        pressureMult = pressureMultiplier
                    )
                }
            }

            // 4. Draw CURRENT FRAME PATHS (Layer by Layer, from Background to Foreground)
            layers.forEach { layer ->
                if (layer.isVisible) {
                    val pathsForFrameAndLayer = pathsMap[currentFrameId]?.filter { it.layerId == layer.id } ?: emptyList()
                    pathsForFrameAndLayer.forEach { drawPathEntity ->
                        drawVectorPath(
                            drawPathEntity = drawPathEntity,
                            overrideColor = null,
                            layerOpacity = layer.opacity,
                            pressureEnabled = isPressureEnabled,
                            pressureMult = pressureMultiplier
                        )
                    }
                }
            }

            // 5. Draw ACTIVE DRAWING PATH (Live Feed)
            if (currentDrawingPoints.isNotEmpty()) {
                val activePathEntity = DrawPathEntity(
                    frameId = currentFrameId,
                    layerId = viewModel.selectedLayerId.value ?: 0,
                    color = brushConfig.color.hashCode(),
                    strokeWidth = brushConfig.size,
                    brushType = brushConfig.type.name,
                    pointsData = "" // Handled live
                )
                drawLivePoints(
                    points = currentDrawingPoints,
                    config = activePathEntity,
                    brushConfigColor = brushConfig.color,
                    pressureEnabled = isPressureEnabled,
                    pressureMult = pressureMultiplier
                )

                if (rulerEnabled && selectedRuler == "MIRR") {
                    val midX = size.width / 2f
                    val mirroredLivePoints = currentDrawingPoints.map {
                        val distToMid = midX - it.x
                        CanvasPoint(midX + distToMid, it.y, it.pressure)
                    }
                    drawLivePoints(
                        points = mirroredLivePoints,
                        config = activePathEntity,
                        brushConfigColor = brushConfig.color,
                        pressureEnabled = isPressureEnabled,
                        pressureMult = pressureMultiplier
                    )
                }
            }

            // 6. Draw RULER GUIDELINES Overlay
            if (rulerEnabled) {
                val cx = rulerCenter.x
                val cy = rulerCenter.y
                val r = rulerRadius
                val guideColor = Color(0xFFFF4081) // Beautiful bright pink accent
                val dashPattern = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

                when (selectedRuler) {
                    "LINE" -> {
                        drawLine(
                            color = guideColor.copy(alpha = 0.5f),
                            start = Offset(0f, cy),
                            end = Offset(size.width, cy),
                            strokeWidth = 3f,
                            pathEffect = dashPattern
                        )
                        drawLine(
                            color = guideColor.copy(alpha = 0.5f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, size.height),
                            strokeWidth = 3f,
                            pathEffect = dashPattern
                        )
                    }
                    "CIRC" -> {
                        drawCircle(
                            color = guideColor.copy(alpha = 0.6f),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3f, pathEffect = dashPattern)
                        )

                        if (!rulerLocked) {
                            // Center handle (Pink Square)
                            drawRect(
                                color = guideColor,
                                topLeft = Offset(cx - 16f, cy - 16f),
                                size = androidx.compose.ui.geometry.Size(32f, 32f)
                            )
                            // Resize handle (Pink Circle on perimeter)
                            drawCircle(
                                color = guideColor,
                                radius = 20f,
                                center = Offset(cx + r, cy)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = Offset(cx + r, cy)
                            )
                        }
                    }
                    "BOX" -> {
                        drawRect(
                            color = guideColor.copy(alpha = 0.6f),
                            topLeft = Offset(cx - r, cy - r),
                            size = androidx.compose.ui.geometry.Size(2f * r, 2f * r),
                            style = Stroke(width = 3f, pathEffect = dashPattern)
                        )

                        if (!rulerLocked) {
                            // Center handle (Pink Square)
                            drawRect(
                                color = guideColor,
                                topLeft = Offset(cx - 16f, cy - 16f),
                                size = androidx.compose.ui.geometry.Size(32f, 32f)
                            )
                            // Resize handle (Pink Circle on bottom-right corner)
                            drawCircle(
                                color = guideColor,
                                radius = 20f,
                                center = Offset(cx + r, cy + r)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = Offset(cx + r, cy + r)
                            )
                        }
                    }
                    "MIRR" -> {
                        val midX = size.width / 2f
                        drawLine(
                            color = guideColor.copy(alpha = 0.7f),
                            start = Offset(midX, 0f),
                            end = Offset(midX, size.height),
                            strokeWidth = 3f,
                            pathEffect = dashPattern
                        )
                        drawCircle(
                            color = guideColor,
                            radius = 12f,
                            center = Offset(midX, 24f)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCheckerboard() {
    val cellSize = 32f
    val cols = (size.width / cellSize).toInt() + 1
    val rows = (size.height / cellSize).toInt() + 1
    for (i in 0 until cols) {
        for (j in 0 until rows) {
            if ((i + j) % 2 == 0) {
                drawRect(
                    color = Color(0xFFF0F0F0),
                    topLeft = Offset(i * cellSize, j * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }
    }
}

private fun DrawScope.drawCustomBackground(backgroundType: String) {
    when (backgroundType) {
        "White" -> {
            drawRect(color = Color.White)
        }
        "Black" -> {
            drawRect(color = Color(0xFF121212))
        }
        "Gray" -> {
            drawRect(color = Color(0xFFD1D5DB))
        }
        "Cream" -> {
            drawRect(color = Color(0xFFFDFBF7))
        }
        "Blue" -> {
            drawRect(color = Color(0xFFE0F2FE))
        }
        "Green" -> {
            drawRect(color = Color(0xFFDCFCE7))
        }
        "Pink" -> {
            drawRect(color = Color(0xFFFCE7F3))
        }
        "None" -> {
            drawRect(color = Color.White)
            drawCheckerboard()
        }
        "Ruled" -> {
            drawRect(color = Color(0xFFFDFBF7))
            val lineSpacing = 40f
            val linesCount = (size.height / lineSpacing).toInt()
            for (i in 1..linesCount) {
                drawLine(
                    color = Color(0xFF93C5FD).copy(alpha = 0.6f),
                    start = Offset(0f, i * lineSpacing),
                    end = Offset(size.width, i * lineSpacing),
                    strokeWidth = 1.5f
                )
            }
            drawLine(
                color = Color(0xFFFCA5A5).copy(alpha = 0.8f),
                start = Offset(120f, 0f),
                end = Offset(120f, size.height),
                strokeWidth = 2f
            )
        }
        "Grid" -> {
            drawRect(color = Color(0xFFFBFBFC))
            val spacing = 40f
            val cols = (size.width / spacing).toInt() + 1
            val rows = (size.height / spacing).toInt() + 1
            for (i in 0 until cols) {
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(i * spacing, 0f),
                    end = Offset(i * spacing, size.height),
                    strokeWidth = 1f
                )
            }
            for (j in 0 until rows) {
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(0f, j * spacing),
                    end = Offset(size.width, j * spacing),
                    strokeWidth = 1f
                )
            }
        }
        "Dotted" -> {
            drawRect(color = Color(0xFFFAFAFA))
            val spacing = 40f
            val cols = (size.width / spacing).toInt() + 1
            val rows = (size.height / spacing).toInt() + 1
            for (i in 1 until cols) {
                for (j in 1 until rows) {
                    drawCircle(
                        color = Color(0xFF9CA3AF).copy(alpha = 0.6f),
                        radius = 2f,
                        center = Offset(i * spacing, j * spacing)
                    )
                }
            }
        }
        "Sketch" -> {
            drawRect(color = Color(0xFFF3F4F6))
            val spacing = 30f
            val lineCount = ((size.width + size.height) / spacing).toInt()
            for (i in 0 until lineCount) {
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(i * spacing, 0f),
                    end = Offset(0f, i * spacing),
                    strokeWidth = 1f
                )
            }
        }
        "Kraft" -> {
            drawRect(color = Color(0xFFD2B48C))
            val spacing = 80f
            val lineCount = ((size.width + size.height) / spacing).toInt()
            for (i in 0 until lineCount) {
                drawLine(
                    color = Color(0xFFC4A484).copy(alpha = 0.4f),
                    start = Offset(i * spacing, 0f),
                    end = Offset(0f, i * spacing),
                    strokeWidth = 1.5f
                )
            }
        }
        "Chalk" -> {
            drawRect(color = Color(0xFF142F24))
            val spacing = 80f
            val cols = (size.width / spacing).toInt() + 1
            val rows = (size.height / spacing).toInt() + 1
            for (i in 0 until cols) {
                drawLine(
                    color = Color(0xFF22543D).copy(alpha = 0.3f),
                    start = Offset(i * spacing, 0f),
                    end = Offset(i * spacing, size.height),
                    strokeWidth = 1.5f
                )
            }
            for (j in 0 until rows) {
                drawLine(
                    color = Color(0xFF22543D).copy(alpha = 0.3f),
                    start = Offset(0f, j * spacing),
                    end = Offset(size.width, j * spacing),
                    strokeWidth = 1.5f
                )
            }
        }
        else -> {
            drawRect(color = Color.White)
        }
    }
}

private fun DrawScope.drawVectorPath(
    drawPathEntity: DrawPathEntity,
    overrideColor: Color?,
    layerOpacity: Float = 1.0f,
    pressureEnabled: Boolean,
    pressureMult: Float
) {
    val points = PathSerializer.deserialize(drawPathEntity.pointsData)
    if (points.isEmpty()) return

    val color = overrideColor ?: Color(drawPathEntity.color).copy(alpha = layerOpacity)
    val isEraser = drawPathEntity.brushType == BrushType.ERASER.name
    val blendMode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver

    drawStrokeSegments(
        points = points,
        baseColor = color,
        baseWidth = drawPathEntity.strokeWidth,
        brushType = BrushType.valueOf(drawPathEntity.brushType),
        blendMode = blendMode,
        pressureEnabled = pressureEnabled,
        pressureMult = pressureMult
    )
}

private fun DrawScope.drawLivePoints(
    points: List<CanvasPoint>,
    config: DrawPathEntity,
    brushConfigColor: Color,
    pressureEnabled: Boolean,
    pressureMult: Float
) {
    val isEraser = config.brushType == BrushType.ERASER.name
    val blendMode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver

    drawStrokeSegments(
        points = points,
        baseColor = brushConfigColor,
        baseWidth = config.strokeWidth,
        brushType = BrushType.valueOf(config.brushType),
        blendMode = blendMode,
        pressureEnabled = pressureEnabled,
        pressureMult = pressureMult
    )
}

private fun DrawScope.drawStrokeSegments(
    points: List<CanvasPoint>,
    baseColor: Color,
    baseWidth: Float,
    brushType: BrushType,
    blendMode: BlendMode,
    pressureEnabled: Boolean,
    pressureMult: Float
) {
    if (points.size < 2) {
        // Draw a single point
        val p = points.firstOrNull() ?: return
        val size = if (pressureEnabled) baseWidth * p.pressure * pressureMult else baseWidth
        drawCircle(
            color = if (blendMode == BlendMode.Clear) Color.Transparent else baseColor,
            radius = size / 2,
            center = p.toOffset(),
            blendMode = blendMode
        )
        return
    }

    // Brush style presets
    val opacity = when (brushType) {
        BrushType.PENCIL -> 0.45f
        BrushType.AIRBRUSH -> 0.15f
        else -> 1.0f
    }
    val finalColor = baseColor.copy(alpha = baseColor.alpha * opacity)

    // For pencil/pen/eraser, we draw connected lines
    if (brushType == BrushType.PEN || brushType == BrushType.ERASER || brushType == BrushType.PENCIL) {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        drawPath(
            path = path,
            color = if (blendMode == BlendMode.Clear) Color.Transparent else finalColor,
            style = Stroke(
                width = baseWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            blendMode = blendMode
        )
    } else if (brushType == BrushType.BRUSH) {
        // Dynamic pressure taper modeling: draw segmented lines with varying thickness
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val avgPressure = (p1.pressure + p2.pressure) / 2
            val dynamicWidth = if (pressureEnabled) {
                (baseWidth * avgPressure * pressureMult).coerceAtLeast(1.5f)
            } else {
                baseWidth
            }

            drawLine(
                color = finalColor,
                start = p1.toOffset(),
                end = p2.toOffset(),
                strokeWidth = dynamicWidth,
                cap = StrokeCap.Round,
                blendMode = blendMode
            )
        }
    } else if (brushType == BrushType.AIRBRUSH) {
        // Soft airbrush model: draw overlapping translucent wider circles along the track
        points.forEach { p ->
            val size = if (pressureEnabled) baseWidth * p.pressure * pressureMult * 2.5f else baseWidth * 2.5f
            drawCircle(
                color = finalColor,
                radius = size / 2,
                center = p.toOffset(),
                blendMode = blendMode
            )
        }
    }
}
