package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DrawPathEntity
import com.example.data.model.FrameEntity
import com.example.data.model.LayerEntity
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimationDao {

    // --- Projects ---
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectByIdFlow(projectId: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)


    // --- Layers ---
    @Query("SELECT * FROM layers WHERE projectId = :projectId ORDER BY layerOrder ASC")
    fun getLayersForProjectFlow(projectId: Long): Flow<List<LayerEntity>>

    @Query("SELECT * FROM layers WHERE projectId = :projectId ORDER BY layerOrder ASC")
    suspend fun getLayersForProject(projectId: Long): List<LayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: LayerEntity): Long

    @Update
    suspend fun updateLayer(layer: LayerEntity)

    @Delete
    suspend fun deleteLayer(layer: LayerEntity)


    // --- Frames ---
    @Query("SELECT * FROM frames WHERE projectId = :projectId ORDER BY sequenceIndex ASC")
    fun getFramesForProjectFlow(projectId: Long): Flow<List<FrameEntity>>

    @Query("SELECT * FROM frames WHERE projectId = :projectId ORDER BY sequenceIndex ASC")
    suspend fun getFramesForProject(projectId: Long): List<FrameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrame(frame: FrameEntity): Long

    @Update
    suspend fun updateFrame(frame: FrameEntity)

    @Delete
    suspend fun deleteFrame(frame: FrameEntity)


    // --- Draw Paths ---
    @Query("SELECT * FROM draw_paths WHERE frameId = :frameId ORDER BY timestamp ASC")
    fun getPathsForFrameFlow(frameId: Long): Flow<List<DrawPathEntity>>

    @Query("SELECT * FROM draw_paths WHERE frameId = :frameId ORDER BY timestamp ASC")
    suspend fun getPathsForFrame(frameId: Long): List<DrawPathEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawPath(drawPath: DrawPathEntity): Long

    @Query("DELETE FROM draw_paths WHERE id = :pathId")
    suspend fun deleteDrawPathById(pathId: Long)

    @Query("DELETE FROM draw_paths WHERE frameId = :frameId")
    suspend fun deletePathsForFrame(frameId: Long)

    @Query("DELETE FROM draw_paths WHERE frameId = :frameId AND layerId = :layerId")
    suspend fun deletePathsForFrameAndLayer(frameId: Long, layerId: Long)
}
