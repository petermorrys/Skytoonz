package com.example.util

import androidx.compose.ui.geometry.Offset

data class CanvasPoint(
    val x: Float,
    val y: Float,
    val pressure: Float
) {
    fun toOffset(): Offset = Offset(x, y)
}

object PathSerializer {

    /**
     * Serializes a list of CanvasPoints to a string format:
     * "x,y,pressure;x,y,pressure;..."
     */
    fun serialize(points: List<CanvasPoint>): String {
        return points.joinToString(separator = ";") { point ->
            "${point.x},${point.y},${point.pressure}"
        }
    }

    /**
     * Deserializes a string format "x,y,pressure;x,y,pressure;..."
     * back into a list of CanvasPoints.
     */
    fun deserialize(data: String): List<CanvasPoint> {
        if (data.isBlank()) return emptyList()
        return try {
            data.split(";").mapNotNull { pointStr ->
                val parts = pointStr.split(",")
                if (parts.size >= 3) {
                    CanvasPoint(
                        x = parts[0].toFloat(),
                        y = parts[1].toFloat(),
                        pressure = parts[2].toFloat()
                    )
                } else if (parts.size == 2) {
                    CanvasPoint(
                        x = parts[0].toFloat(),
                        y = parts[1].toFloat(),
                        pressure = 1.0f
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
