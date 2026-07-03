package com.example.data.repository

import com.example.data.dao.AnimationDao
import com.example.data.model.DrawPathEntity
import com.example.data.model.FrameEntity
import com.example.data.model.LayerEntity
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

class AnimationRepository(private val animationDao: AnimationDao) {

    val allProjects: Flow<List<ProjectEntity>> = animationDao.getAllProjectsFlow()

    fun getProjectByIdFlow(projectId: Long): Flow<ProjectEntity?> {
        return animationDao.getProjectByIdFlow(projectId)
    }

    suspend fun getProjectById(projectId: Long): ProjectEntity? {
        return animationDao.getProjectById(projectId)
    }

    fun getLayersForProjectFlow(projectId: Long): Flow<List<LayerEntity>> {
        return animationDao.getLayersForProjectFlow(projectId)
    }

    suspend fun getLayersForProject(projectId: Long): List<LayerEntity> {
        return animationDao.getLayersForProject(projectId)
    }

    fun getFramesForProjectFlow(projectId: Long): Flow<List<FrameEntity>> {
        return animationDao.getFramesForProjectFlow(projectId)
    }

    suspend fun getFramesForProject(projectId: Long): List<FrameEntity> {
        return animationDao.getFramesForProject(projectId)
    }

    fun getPathsForFrameFlow(frameId: Long): Flow<List<DrawPathEntity>> {
        return animationDao.getPathsForFrameFlow(frameId)
    }

    suspend fun getPathsForFrame(frameId: Long): List<DrawPathEntity> {
        return animationDao.getPathsForFrame(frameId)
    }

    /**
     * Creates a new project and populates it with:
     * 1. Three default layers (Background, Layer 1, Foreground).
     * 2. A default first frame.
     */
    suspend fun createProject(name: String, fps: Int): Long {
        val projectId = animationDao.insertProject(
            ProjectEntity(
                name = name,
                fps = fps,
                isSynced = false
            )
        )

        // Create 3 layers
        val bgLayerId = animationDao.insertLayer(
            LayerEntity(
                projectId = projectId,
                name = "Background",
                layerOrder = 0,
                opacity = 0.8f
            )
        )
        val l1LayerId = animationDao.insertLayer(
            LayerEntity(
                projectId = projectId,
                name = "Layer 1",
                layerOrder = 1,
                opacity = 1.0f
            )
        )
        val fgLayerId = animationDao.insertLayer(
            LayerEntity(
                projectId = projectId,
                name = "Foreground",
                layerOrder = 2,
                opacity = 1.0f
            )
        )

        // Create the first frame
        animationDao.insertFrame(
            FrameEntity(
                projectId = projectId,
                sequenceIndex = 0
            )
        )

        return projectId
    }

    suspend fun updateProject(project: ProjectEntity) {
        animationDao.updateProject(project)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        animationDao.deleteProject(project)
    }

    suspend fun insertLayer(layer: LayerEntity): Long {
        return animationDao.insertLayer(layer)
    }

    suspend fun updateLayer(layer: LayerEntity) {
        animationDao.updateLayer(layer)
    }

    suspend fun deleteLayer(layer: LayerEntity) {
        animationDao.deleteLayer(layer)
    }

    suspend fun insertFrame(frame: FrameEntity): Long {
        return animationDao.insertFrame(frame)
    }

    suspend fun updateFrame(frame: FrameEntity) {
        animationDao.updateFrame(frame)
    }

    suspend fun deleteFrame(frame: FrameEntity) {
        animationDao.deleteFrame(frame)
    }

    suspend fun insertDrawPath(drawPath: DrawPathEntity): Long {
        return animationDao.insertDrawPath(drawPath)
    }

    suspend fun deleteDrawPathById(pathId: Long) {
        animationDao.deleteDrawPathById(pathId)
    }

    suspend fun deletePathsForFrameAndLayer(frameId: Long, layerId: Long) {
        animationDao.deletePathsForFrameAndLayer(frameId, layerId)
    }

    suspend fun deletePathsForFrame(frameId: Long) {
        animationDao.deletePathsForFrame(frameId)
    }
}
