package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class AppIconType {
    ADD,
    ARROW_BACK,
    UNDO,
    REDO,
    SETTINGS,
    SHARE,
    DELETE,
    FOLDER_OPEN,
    MOVIE,
    MUSIC_NOTE,
    MUSIC_VIDEO,
    VOLUME_UP,
    CLOUD_DONE,
    CLOUD_QUEUE,
    CLOUD_UPLOAD,
    CHECK_CIRCLE,
    VIDEO_LIBRARY,
    TASK_ALT,
    CLOSE,
    VISIBILITY,
    VISIBILITY_OFF,
    MIC,
    MIC_OFF,
    GIF,
    VIDEO_FILE,
    CODE,
    CONTENT_COPY,
    SKIP_PREVIOUS,
    SKIP_NEXT,
    PAUSE,
    PLAY_ARROW,
    BRUSH,
    CREATE, // pencil
    GESTURE,
    GRAIN,
    AUTO_FIX_NORMAL,
    OUTLINED_DELETE,
    LAYERS,
    LOCK,
    LOCK_OPEN,
    CONTENT_PASTE,
    RULER,
    LINE,
    CIRCLE,
    BOX,
    MIRROR,
    DRAG_INDICATOR,
    FORMAT_COLOR_FILL,
    TITLE,
    ARROW_LEFT,
    ARROW_RIGHT
}

@Composable
fun AppIcon(
    icon: AppIconType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    // Attempt standard Core icons first to reduce custom rendering
    val standardVector = when (icon) {
        AppIconType.ADD -> Icons.Default.Add
        AppIconType.ARROW_BACK -> Icons.Default.ArrowBack
        AppIconType.CLOSE -> Icons.Default.Close
        AppIconType.DELETE -> Icons.Default.Delete
        AppIconType.PLAY_ARROW -> Icons.Default.PlayArrow
        AppIconType.SETTINGS -> Icons.Default.Settings
        AppIconType.SHARE -> Icons.Default.Share
        AppIconType.LOCK -> Icons.Default.Lock
        AppIconType.ARROW_LEFT -> Icons.Default.KeyboardArrowLeft
        AppIconType.ARROW_RIGHT -> Icons.Default.KeyboardArrowRight
        else -> null
    }

    if (standardVector != null) {
        Icon(
            imageVector = standardVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .semantics {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val color = tint

                when (icon) {
                    AppIconType.UNDO -> {
                        val undoPath = Path().apply {
                            moveTo(w * 0.3f, h * 0.45f)
                            quadraticTo(w * 0.65f, h * 0.45f, w * 0.65f, h * 0.72f)
                        }
                        drawPath(undoPath, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round))
                        val undoHead = Path().apply {
                            moveTo(w * 0.45f, h * 0.3f)
                            lineTo(w * 0.3f, h * 0.45f)
                            lineTo(w * 0.45f, h * 0.6f)
                        }
                        drawPath(undoHead, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.REDO -> {
                        val redoPath = Path().apply {
                            moveTo(w * 0.7f, h * 0.45f)
                            quadraticTo(w * 0.35f, h * 0.45f, w * 0.35f, h * 0.72f)
                        }
                        drawPath(redoPath, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round))
                        val redoHead = Path().apply {
                            moveTo(w * 0.55f, h * 0.3f)
                            lineTo(w * 0.7f, h * 0.45f)
                            lineTo(w * 0.55f, h * 0.6f)
                        }
                        drawPath(redoHead, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.CLOUD_DONE -> {
                        val cloudPath = Path().apply {
                            moveTo(w * 0.25f, h * 0.7f)
                            arcTo(Rect(w * 0.15f, h * 0.45f, w * 0.45f, h * 0.75f), 90f, 180f, false)
                            arcTo(Rect(w * 0.3f, h * 0.25f, w * 0.7f, h * 0.65f), 180f, 180f, false)
                            arcTo(Rect(w * 0.55f, h * 0.45f, w * 0.85f, h * 0.75f), 270f, 180f, false)
                            close()
                        }
                        drawPath(cloudPath, color)
                        val checkPath = Path().apply {
                            moveTo(w * 0.42f, h * 0.58f)
                            lineTo(w * 0.5f, h * 0.66f)
                            lineTo(w * 0.65f, h * 0.5f)
                        }
                        drawPath(checkPath, Color.White, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.CLOUD_QUEUE -> {
                        val cloudPath = Path().apply {
                            moveTo(w * 0.25f, h * 0.7f)
                            arcTo(Rect(w * 0.15f, h * 0.45f, w * 0.45f, h * 0.75f), 90f, 180f, false)
                            arcTo(Rect(w * 0.3f, h * 0.25f, w * 0.7f, h * 0.65f), 180f, 180f, false)
                            arcTo(Rect(w * 0.55f, h * 0.45f, w * 0.85f, h * 0.75f), 270f, 180f, false)
                            close()
                        }
                        drawPath(cloudPath, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.CLOUD_UPLOAD -> {
                        val cloudPath = Path().apply {
                            moveTo(w * 0.25f, h * 0.7f)
                            arcTo(Rect(w * 0.15f, h * 0.45f, w * 0.45f, h * 0.75f), 90f, 180f, false)
                            arcTo(Rect(w * 0.3f, h * 0.25f, w * 0.7f, h * 0.65f), 180f, 180f, false)
                            arcTo(Rect(w * 0.55f, h * 0.45f, w * 0.85f, h * 0.75f), 270f, 180f, false)
                            close()
                        }
                        drawPath(cloudPath, color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
                        drawLine(color, Offset(w * 0.5f, h * 0.4f), Offset(w * 0.5f, h * 0.68f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                        val arrHead = Path().apply {
                            moveTo(w * 0.38f, h * 0.5f)
                            lineTo(w * 0.5f, h * 0.38f)
                            lineTo(w * 0.62f, h * 0.5f)
                        }
                        drawPath(arrHead, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.FOLDER_OPEN -> {
                        val folderPath = Path().apply {
                            moveTo(w * 0.15f, h * 0.3f)
                            lineTo(w * 0.4f, h * 0.3f)
                            lineTo(w * 0.5f, h * 0.4f)
                            lineTo(w * 0.85f, h * 0.4f)
                            lineTo(w * 0.85f, h * 0.75f)
                            lineTo(w * 0.15f, h * 0.75f)
                            close()
                        }
                        drawPath(folderPath, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawLine(color, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.75f, h * 0.5f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                    }
                    AppIconType.MOVIE -> {
                        val moviePath = Path().apply {
                            addRoundRect(RoundRect(w * 0.2f, h * 0.25f, w * 0.8f, h * 0.75f, CornerRadius(w * 0.08f)))
                        }
                        drawPath(moviePath, color, style = Stroke(width = w * 0.1f))
                        drawLine(color, Offset(w * 0.2f, h * 0.42f), Offset(w * 0.8f, h * 0.42f), strokeWidth = w * 0.08f)
                        drawLine(color, Offset(w * 0.4f, h * 0.25f), Offset(w * 0.45f, h * 0.42f), strokeWidth = w * 0.06f)
                        drawLine(color, Offset(w * 0.6f, h * 0.25f), Offset(w * 0.65f, h * 0.42f), strokeWidth = w * 0.06f)
                    }
                    AppIconType.MUSIC_NOTE -> {
                        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.35f, h * 0.7f))
                        drawLine(color, Offset(w * 0.46f, h * 0.7f), Offset(w * 0.46f, h * 0.25f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                        val flagPath = Path().apply {
                            moveTo(w * 0.46f, h * 0.25f)
                            quadraticTo(w * 0.65f, h * 0.3f, w * 0.65f, h * 0.45f)
                            quadraticTo(w * 0.55f, h * 0.45f, w * 0.46f, h * 0.38f)
                        }
                        drawPath(flagPath, color)
                    }
                    AppIconType.MUSIC_VIDEO -> {
                        drawRect(color, topLeft = Offset(w * 0.2f, h * 0.2f), size = Size(w * 0.6f, h * 0.6f), style = Stroke(width = w * 0.08f))
                        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.42f, h * 0.62f))
                        drawLine(color, Offset(w * 0.48f, h * 0.62f), Offset(w * 0.48f, h * 0.38f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                        drawLine(color, Offset(w * 0.48f, h * 0.38f), Offset(w * 0.62f, h * 0.38f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                    }
                    AppIconType.VOLUME_UP -> {
                        val speakerPath = Path().apply {
                            moveTo(w * 0.2f, h * 0.4f)
                            lineTo(w * 0.35f, h * 0.4f)
                            lineTo(w * 0.55f, h * 0.25f)
                            lineTo(w * 0.55f, h * 0.75f)
                            lineTo(w * 0.35f, h * 0.6f)
                            lineTo(w * 0.2f, h * 0.6f)
                            close()
                        }
                        drawPath(speakerPath, color)
                        drawArc(color, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w * 0.45f, h * 0.25f), size = Size(w * 0.3f, h * 0.5f), style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                        drawArc(color, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w * 0.35f, h * 0.15f), size = Size(w * 0.5f, h * 0.7f), style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                    }
                    AppIconType.CHECK_CIRCLE -> {
                        drawCircle(color, radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w * 0.08f))
                        val chPath = Path().apply {
                            moveTo(w * 0.35f, h * 0.5f)
                            lineTo(w * 0.47f, h * 0.62f)
                            lineTo(w * 0.68f, h * 0.38f)
                        }
                        drawPath(chPath, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.VIDEO_LIBRARY -> {
                        drawRect(color, topLeft = Offset(w * 0.25f, h * 0.15f), size = Size(w * 0.55f, h * 0.55f), style = Stroke(width = w * 0.08f))
                        drawRect(color, topLeft = Offset(w * 0.15f, h * 0.25f), size = Size(w * 0.55f, h * 0.55f), style = Stroke(width = w * 0.08f))
                    }
                    AppIconType.TASK_ALT -> {
                        drawCircle(color, radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w * 0.08f))
                        val chPath = Path().apply {
                            moveTo(w * 0.35f, h * 0.5f)
                            lineTo(w * 0.47f, h * 0.62f)
                            lineTo(w * 0.68f, h * 0.38f)
                        }
                        drawPath(chPath, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.VISIBILITY -> {
                        val eyePath = Path().apply {
                            moveTo(w * 0.15f, h * 0.5f)
                            quadraticTo(w * 0.5f, h * 0.2f, w * 0.85f, h * 0.5f)
                            quadraticTo(w * 0.5f, h * 0.8f, w * 0.15f, h * 0.5f)
                        }
                        drawPath(eyePath, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.5f))
                    }
                    AppIconType.VISIBILITY_OFF -> {
                        val eyePath = Path().apply {
                            moveTo(w * 0.15f, h * 0.5f)
                            quadraticTo(w * 0.5f, h * 0.2f, w * 0.85f, h * 0.5f)
                            quadraticTo(w * 0.5f, h * 0.8f, w * 0.15f, h * 0.5f)
                        }
                        drawPath(eyePath, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.5f))
                        drawLine(color, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                    }
                    AppIconType.MIC -> {
                        val micCapsule = Path().apply {
                            addRoundRect(RoundRect(w * 0.38f, h * 0.25f, w * 0.62f, h * 0.6f, CornerRadius(w * 0.12f)))
                        }
                        drawPath(micCapsule, color, style = Stroke(width = w * 0.08f))
                        drawArc(color, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.28f, h * 0.4f), size = Size(w * 0.44f, h * 0.3f), style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                        drawLine(color, Offset(w * 0.5f, h * 0.7f), Offset(w * 0.5f, h * 0.85f), strokeWidth = w * 0.08f)
                    }
                    AppIconType.MIC_OFF -> {
                        val micCapsule = Path().apply {
                            addRoundRect(RoundRect(w * 0.38f, h * 0.25f, w * 0.62f, h * 0.6f, CornerRadius(w * 0.12f)))
                        }
                        drawPath(micCapsule, color, style = Stroke(width = w * 0.08f))
                        drawArc(color, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.28f, h * 0.4f), size = Size(w * 0.44f, h * 0.3f), style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                        drawLine(color, Offset(w * 0.5f, h * 0.7f), Offset(w * 0.5f, h * 0.85f), strokeWidth = w * 0.08f)
                        drawLine(color, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                    }
                    AppIconType.GIF -> {
                        drawRect(color, topLeft = Offset(w * 0.15f, h * 0.25f), size = Size(w * 0.7f, h * 0.5f), style = Stroke(width = w * 0.08f))
                        val gPath = Path().apply {
                            moveTo(w * 0.4f, h * 0.45f)
                            lineTo(w * 0.3f, h * 0.45f)
                            arcTo(Rect(w * 0.24f, h * 0.35f, w * 0.42f, h * 0.65f), 0f, 270f, false)
                        }
                        drawPath(gPath, color, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
                        drawLine(color, Offset(w * 0.52f, h * 0.38f), Offset(w * 0.52f, h * 0.62f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                        val fPath = Path().apply {
                            moveTo(w * 0.75f, h * 0.38f)
                            lineTo(w * 0.63f, h * 0.38f)
                            lineTo(w * 0.63f, h * 0.62f)
                            moveTo(w * 0.63f, h * 0.5f)
                            lineTo(w * 0.72f, h * 0.5f)
                        }
                        drawPath(fPath, color, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
                    }
                    AppIconType.VIDEO_FILE -> {
                        val docPath = Path().apply {
                            moveTo(w * 0.25f, h * 0.2f)
                            lineTo(w * 0.6f, h * 0.2f)
                            lineTo(w * 0.75f, h * 0.35f)
                            lineTo(w * 0.75f, h * 0.8f)
                            lineTo(w * 0.25f, h * 0.8f)
                            close()
                        }
                        drawPath(docPath, color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
                        val playPath = Path().apply {
                            moveTo(w * 0.42f, h * 0.42f)
                            lineTo(w * 0.62f, h * 0.52f)
                            lineTo(w * 0.42f, h * 0.62f)
                            close()
                        }
                        drawPath(playPath, color)
                    }
                    AppIconType.CODE -> {
                        val ltPath = Path().apply {
                            moveTo(w * 0.4f, h * 0.35f)
                            lineTo(w * 0.22f, h * 0.5f)
                            lineTo(w * 0.4f, h * 0.65f)
                        }
                        drawPath(ltPath, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        val gtPath = Path().apply {
                            moveTo(w * 0.6f, h * 0.35f)
                            lineTo(w * 0.78f, h * 0.5f)
                            lineTo(w * 0.6f, h * 0.65f)
                        }
                        drawPath(gtPath, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    AppIconType.CONTENT_COPY -> {
                        drawRect(color, topLeft = Offset(w * 0.38f, h * 0.18f), size = Size(w * 0.44f, h * 0.48f), style = Stroke(width = w * 0.08f))
                        drawRect(color, topLeft = Offset(w * 0.18f, h * 0.34f), size = Size(w * 0.44f, h * 0.48f), style = Stroke(width = w * 0.08f))
                    }
                    AppIconType.SKIP_PREVIOUS -> {
                        drawLine(color, Offset(w * 0.25f, h * 0.3f), Offset(w * 0.25f, h * 0.7f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                        val triPath = Path().apply {
                            moveTo(w * 0.7f, h * 0.3f)
                            lineTo(w * 0.35f, h * 0.5f)
                            lineTo(w * 0.7f, h * 0.7f)
                            close()
                        }
                        drawPath(triPath, color)
                    }
                    AppIconType.SKIP_NEXT -> {
                        val triPath = Path().apply {
                            moveTo(w * 0.3f, h * 0.3f)
                            lineTo(w * 0.65f, h * 0.5f)
                            lineTo(w * 0.3f, h * 0.7f)
                            close()
                        }
                        drawPath(triPath, color)
                        drawLine(color, Offset(w * 0.75f, h * 0.3f), Offset(w * 0.75f, h * 0.7f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                    }
                    AppIconType.BRUSH -> {
                        val brushPath = Path().apply {
                            moveTo(w * 0.25f, h * 0.75f)
                            lineTo(w * 0.55f, h * 0.45f)
                        }
                        drawPath(brushPath, color, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round))
                        val tipPath = Path().apply {
                            moveTo(w * 0.55f, h * 0.45f)
                            lineTo(w * 0.75f, h * 0.25f)
                            lineTo(w * 0.65f, h * 0.4f)
                            close()
                        }
                        drawPath(tipPath, color)
                    }
                    AppIconType.GESTURE -> {
                        val gesturePath = Path().apply {
                            moveTo(w * 0.2f, h * 0.6f)
                            cubicTo(w * 0.4f, h * 0.2f, w * 0.6f, h * 0.8f, w * 0.8f, h * 0.4f)
                        }
                        drawPath(gesturePath, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round))
                    }
                    AppIconType.GRAIN -> {
                        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.3f, h * 0.3f))
                        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.25f))
                        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.7f, h * 0.35f))
                        drawCircle(color, radius = w * 0.4f, center = Offset(w * 0.4f, h * 0.6f), style = Stroke(width = w * 0.01f)) // just small dots
                        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.6f, h * 0.7f))
                        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.75f, h * 0.55f))
                    }
                    AppIconType.AUTO_FIX_NORMAL -> {
                        drawLine(color, Offset(w * 0.25f, h * 0.75f), Offset(w * 0.6f, h * 0.4f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                        drawLine(color, Offset(w * 0.7f, h * 0.15f), Offset(w * 0.7f, h * 0.35f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                        drawLine(color, Offset(w * 0.6f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                    }
                    AppIconType.OUTLINED_DELETE -> {
                        drawRect(color, topLeft = Offset(w * 0.3f, h * 0.35f), size = Size(w * 0.4f, h * 0.45f), style = Stroke(width = w * 0.08f))
                        drawLine(color, Offset(w * 0.2f, h * 0.35f), Offset(w * 0.8f, h * 0.35f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                        drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.41f, h * 0.23f), size = Size(w * 0.18f, h * 0.12f), style = Stroke(width = w * 0.08f))
                    }
                    AppIconType.LAYERS -> {
                        fun DrawScope.drawLayerP(topY: Float, tintColor: Color, fill: Boolean) {
                            val path = Path().apply {
                                moveTo(w * 0.5f, topY)
                                lineTo(w * 0.85f, topY + h * 0.15f)
                                lineTo(w * 0.5f, topY + h * 0.3f)
                                lineTo(w * 0.15f, topY + h * 0.15f)
                                close()
                            }
                            if (fill) {
                                drawPath(path, tintColor)
                            } else {
                                drawPath(path, tintColor, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
                            }
                        }
                        drawLayerP(h * 0.45f, color, false)
                        drawLayerP(h * 0.25f, color, true)
                    }
                    AppIconType.PAUSE -> {
                        drawRect(color, topLeft = Offset(w * 0.3f, h * 0.25f), size = Size(w * 0.12f, h * 0.5f))
                        drawRect(color, topLeft = Offset(w * 0.58f, h * 0.25f), size = Size(w * 0.12f, h * 0.5f))
                    }
                    AppIconType.CREATE -> {
                        val pencilPath = Path().apply {
                            moveTo(w * 0.25f, h * 0.75f)
                            lineTo(w * 0.35f, h * 0.75f)
                            lineTo(w * 0.75f, h * 0.35f)
                            lineTo(w * 0.65f, h * 0.25f)
                            lineTo(w * 0.25f, h * 0.65f)
                            close()
                        }
                        drawPath(pencilPath, color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
                        val leadPath = Path().apply {
                            moveTo(w * 0.25f, h * 0.65f)
                            lineTo(w * 0.25f, h * 0.75f)
                            lineTo(w * 0.35f, h * 0.75f)
                            close()
                        }
                        drawPath(leadPath, color)
                    }
                    AppIconType.LOCK_OPEN -> {
                        // Draw body
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(w * 0.25f, h * 0.45f),
                            size = Size(w * 0.5f, h * 0.35f),
                            cornerRadius = CornerRadius(w * 0.08f)
                        )
                        // Draw open shackle
                        val shacklePath = Path().apply {
                            moveTo(w * 0.35f, h * 0.45f)
                            lineTo(w * 0.35f, h * 0.3f)
                            arcTo(Rect(w * 0.35f, h * 0.15f, w * 0.65f, h * 0.45f), 180f, 180f, false)
                            lineTo(w * 0.65f, h * 0.32f)
                        }
                        drawPath(shacklePath, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                    }
                    AppIconType.CONTENT_PASTE -> {
                        // Draw clipboard backing board
                        val boardPath = Path().apply {
                            addRoundRect(RoundRect(w * 0.25f, h * 0.25f, w * 0.75f, h * 0.85f, CornerRadius(w * 0.08f)))
                        }
                        drawPath(boardPath, color, style = Stroke(width = w * 0.08f))
                        // Draw paper clip header
                        drawRect(
                            color = color,
                            topLeft = Offset(w * 0.4f, h * 0.18f),
                            size = Size(w * 0.2f, h * 0.12f)
                        )
                        // Draw text lines
                        drawLine(color, Offset(w * 0.35f, h * 0.45f), Offset(w * 0.65f, h * 0.45f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                        drawLine(color, Offset(w * 0.35f, h * 0.6f), Offset(w * 0.65f, h * 0.6f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                    }
                    AppIconType.RULER -> {
                        val rulerPath = Path().apply {
                            moveTo(w * 0.15f, h * 0.35f)
                            lineTo(w * 0.85f, h * 0.35f)
                            lineTo(w * 0.85f, h * 0.65f)
                            lineTo(w * 0.15f, h * 0.65f)
                            close()
                        }
                        drawPath(rulerPath, color, style = Stroke(width = w * 0.08f))
                        drawLine(color, Offset(w * 0.3f, h * 0.35f), Offset(w * 0.3f, h * 0.48f), strokeWidth = w * 0.06f)
                        drawLine(color, Offset(w * 0.45f, h * 0.35f), Offset(w * 0.45f, h * 0.52f), strokeWidth = w * 0.06f)
                        drawLine(color, Offset(w * 0.6f, h * 0.35f), Offset(w * 0.6f, h * 0.48f), strokeWidth = w * 0.06f)
                        drawLine(color, Offset(w * 0.75f, h * 0.35f), Offset(w * 0.75f, h * 0.52f), strokeWidth = w * 0.06f)
                    }
                    AppIconType.LINE -> {
                        drawLine(color, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.2f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                    }
                    AppIconType.CIRCLE -> {
                        drawCircle(color, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w * 0.08f))
                    }
                    AppIconType.BOX -> {
                        drawRect(color, topLeft = Offset(w * 0.2f, h * 0.2f), size = Size(w * 0.6f, h * 0.6f), style = Stroke(width = w * 0.08f))
                    }
                    AppIconType.MIRROR -> {
                        val dashPattern = PathEffect.dashPathEffect(floatArrayOf(w * 0.1f, w * 0.1f), 0f)
                        drawLine(color, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.85f), strokeWidth = w * 0.06f, pathEffect = dashPattern)
                        val leftTri = Path().apply {
                            moveTo(w * 0.35f, h * 0.35f)
                            lineTo(w * 0.15f, h * 0.5f)
                            lineTo(w * 0.35f, h * 0.65f)
                            close()
                        }
                        drawPath(leftTri, color)
                        val rightTri = Path().apply {
                            moveTo(w * 0.65f, h * 0.35f)
                            lineTo(w * 0.85f, h * 0.5f)
                            lineTo(w * 0.65f, h * 0.65f)
                            close()
                        }
                        drawPath(rightTri, color)
                    }
                    AppIconType.DRAG_INDICATOR -> {
                        val r = w * 0.06f
                        drawCircle(color, radius = r, center = Offset(w * 0.35f, h * 0.25f))
                        drawCircle(color, radius = r, center = Offset(w * 0.65f, h * 0.25f))
                        drawCircle(color, radius = r, center = Offset(w * 0.35f, h * 0.5f))
                        drawCircle(color, radius = r, center = Offset(w * 0.65f, h * 0.5f))
                        drawCircle(color, radius = r, center = Offset(w * 0.35f, h * 0.75f))
                        drawCircle(color, radius = r, center = Offset(w * 0.65f, h * 0.75f))
                    }
                    AppIconType.FORMAT_COLOR_FILL -> {
                        val bucket = Path().apply {
                            moveTo(w * 0.3f, h * 0.35f)
                            lineTo(w * 0.55f, h * 0.15f)
                            lineTo(w * 0.85f, h * 0.45f)
                            lineTo(w * 0.6f, h * 0.65f)
                            close()
                        }
                        drawPath(bucket, color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
                        val handle = Path().apply {
                            moveTo(w * 0.3f, h * 0.35f)
                            quadraticTo(w * 0.55f, h * 0.6f, w * 0.85f, h * 0.45f)
                        }
                        drawPath(handle, color, style = Stroke(width = w * 0.06f))
                        val drip = Path().apply {
                            moveTo(w * 0.58f, h * 0.65f)
                            quadraticTo(w * 0.58f, h * 0.85f, w * 0.48f, h * 0.85f)
                            quadraticTo(w * 0.38f, h * 0.85f, w * 0.38f, h * 0.75f)
                            close()
                        }
                        drawPath(drip, color)
                    }
                    AppIconType.TITLE -> {
                        drawLine(color, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                        drawLine(color, Offset(w * 0.5f, h * 0.25f), Offset(w * 0.5f, h * 0.8f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                    }
                    else -> {}
                }
            }
        }
    }
}
