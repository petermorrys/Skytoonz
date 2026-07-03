package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fps: Int = 12,
    val width: Int = 1080,
    val height: Int = 1080,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val audioTrackName: String? = null,
    val audioTrackDurationMs: Long = 0
)

@Entity(
    tableName = "layers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class LayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val opacity: Float = 1.0f,
    val isVisible: Boolean = true,
    val layerOrder: Int
)

@Entity(
    tableName = "frames",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class FrameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val sequenceIndex: Int
)

@Entity(
    tableName = "draw_paths",
    foreignKeys = [
        ForeignKey(
            entity = FrameEntity::class,
            parentColumns = ["id"],
            childColumns = ["frameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["layerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("frameId"), Index("layerId")]
)
data class DrawPathEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val frameId: Long,
    val layerId: Long,
    val color: Int,
    val strokeWidth: Float,
    val brushType: String, // PEN, PENCIL, BRUSH, AIRBRUSH, ERASER
    val pointsData: String, // Format: "x,y,pressure;x,y,pressure;..."
    val timestamp: Long = System.currentTimeMillis()
)
