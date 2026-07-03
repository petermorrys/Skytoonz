package com.example.ui.drawing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

enum class BrushType(val label: String) {
    PEN("Pen"),
    PENCIL("Pencil"),
    BRUSH("Brush"),
    AIRBRUSH("Airbrush"),
    ERASER("Eraser")
}

data class BrushConfig(
    val type: BrushType = BrushType.PEN,
    val color: Color = Color.Black,
    val size: Float = 12f,
    val opacity: Float = 1.0f
)
