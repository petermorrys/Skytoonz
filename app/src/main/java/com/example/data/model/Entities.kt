package com.example.data.model

data class ProjectEntity(
    val id: Long = 0,
    val name: String,
    val fps: Int = 12,
    val width: Int = 1080,
    val height: Int = 1080,
    val backgroundType: String = "White",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val audioTrackName: String? = null,
    val audioTrackDurationMs: Long = 0
)

data class LayerEntity(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val opacity: Float = 1.0f,
    val isVisible: Boolean = true,
    val layerOrder: Int
)

data class FrameEntity(
    val id: Long = 0,
    val projectId: Long,
    val sequenceIndex: Int
)

data class DrawPathEntity(
    val id: Long = 0,
    val frameId: Long,
    val layerId: Long,
    val color: Int,
    val strokeWidth: Float,
    val brushType: String, // PEN, PENCIL, BRUSH, AIRBRUSH, ERASER
    val pointsData: String, // Format: "x,y,pressure;x,y,pressure;..."
    val timestamp: Long = System.currentTimeMillis()
)

