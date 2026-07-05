package com.example.data.repository

import com.example.data.model.DrawPathEntity
import com.example.data.model.FrameEntity
import com.example.data.model.LayerEntity
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class AnimationRepository {

    private val projectsFlow = MutableStateFlow<List<ProjectEntity>>(emptyList())
    private val layersFlow = MutableStateFlow<List<LayerEntity>>(emptyList())
    private val framesFlow = MutableStateFlow<List<FrameEntity>>(emptyList())
    private val pathsFlow = MutableStateFlow<List<DrawPathEntity>>(emptyList())

    private val idGenerator = java.util.concurrent.atomic.AtomicLong(10000)

    init {
        // Create pre-populated Demo project "Demo - Cat Jump"
        val demoProjectId = 9999L
        val demoProject = ProjectEntity(
            id = demoProjectId,
            name = "Demo - Cat Jump",
            fps = 12,
            width = 1920,
            height = 1080,
            backgroundType = "Grid",
            createdAt = System.currentTimeMillis() - 1000 * 60,
            isSynced = true
        )
        projectsFlow.value = listOf(demoProject)

        // Insert layers for Demo
        layersFlow.value = listOf(
            LayerEntity(id = 100L, projectId = demoProjectId, name = "Background", layerOrder = 0, opacity = 0.8f),
            LayerEntity(id = 101L, projectId = demoProjectId, name = "Layer 1", layerOrder = 1, opacity = 1.0f),
            LayerEntity(id = 102L, projectId = demoProjectId, name = "Foreground", layerOrder = 2, opacity = 1.0f)
        )

        // Insert 3 frames to simulate a jumping motion
        val frameId1 = 200L
        val frameId2 = 201L
        val frameId3 = 202L
        framesFlow.value = listOf(
            FrameEntity(id = frameId1, projectId = demoProjectId, sequenceIndex = 0),
            FrameEntity(id = frameId2, projectId = demoProjectId, sequenceIndex = 1),
            FrameEntity(id = frameId3, projectId = demoProjectId, sequenceIndex = 2)
        )

        // Paths for Frame 1 (Cat sitting low) - drawn with basic vectors
        val path1 = DrawPathEntity(
            id = 300L,
            frameId = frameId1,
            layerId = 101L,
            color = 0xFFEC4899.toInt(), // Beautiful pink
            strokeWidth = 14f,
            brushType = "PEN",
            pointsData = "300,500,1.0;320,530,1.0;350,550,1.0;380,530,1.0;400,500,1.0;350,470,1.0;300,500,1.0;330,470,1.0;350,430,1.0;370,470,1.0" // Body + Ears outline
        )
        // Paths for Frame 2 (Jumping high)
        val path2 = DrawPathEntity(
            id = 301L,
            frameId = frameId2,
            layerId = 101L,
            color = 0xFFEC4899.toInt(),
            strokeWidth = 14f,
            brushType = "PEN",
            pointsData = "500,300,1.0;520,330,1.0;550,350,1.0;580,330,1.0;600,300,1.0;550,270,1.0;500,300,1.0;530,270,1.0;550,230,1.0;570,270,1.0"
        )
        // Paths for Frame 3 (Landing)
        val path3 = DrawPathEntity(
            id = 302L,
            frameId = frameId3,
            layerId = 101L,
            color = 0xFFEC4899.toInt(),
            strokeWidth = 14f,
            brushType = "PEN",
            pointsData = "700,500,1.0;720,530,1.0;750,550,1.0;780,530,1.0;800,500,1.0;750,470,1.0;700,500,1.0;730,470,1.0;750,430,1.0;770,470,1.0"
        )
        pathsFlow.value = listOf(path1, path2, path3)
    }

    val allProjects: Flow<List<ProjectEntity>> = projectsFlow.map { list ->
        list.sortedByDescending { it.createdAt }
    }

    fun getProjectByIdFlow(projectId: Long): Flow<ProjectEntity?> {
        return projectsFlow.map { list ->
            list.find { it.id == projectId }
        }
    }

    suspend fun getProjectById(projectId: Long): ProjectEntity? {
        return projectsFlow.value.find { it.id == projectId }
    }

    fun getLayersForProjectFlow(projectId: Long): Flow<List<LayerEntity>> {
        return layersFlow.map { list ->
            list.filter { it.projectId == projectId }.sortedBy { it.layerOrder }
        }
    }

    suspend fun getLayersForProject(projectId: Long): List<LayerEntity> {
        return layersFlow.value.filter { it.projectId == projectId }.sortedBy { it.layerOrder }
    }

    fun getFramesForProjectFlow(projectId: Long): Flow<List<FrameEntity>> {
        return framesFlow.map { list ->
            list.filter { it.projectId == projectId }.sortedBy { it.sequenceIndex }
        }
    }

    suspend fun getFramesForProject(projectId: Long): List<FrameEntity> {
        return framesFlow.value.filter { it.projectId == projectId }.sortedBy { it.sequenceIndex }
    }

    fun getPathsForFrameFlow(frameId: Long): Flow<List<DrawPathEntity>> {
        return pathsFlow.map { list ->
            list.filter { it.frameId == frameId }.sortedBy { it.timestamp }
        }
    }

    suspend fun getPathsForFrame(frameId: Long): List<DrawPathEntity> {
        return pathsFlow.value.filter { it.frameId == frameId }.sortedBy { it.timestamp }
    }

    suspend fun createProject(
        name: String,
        fps: Int,
        width: Int = 1080,
        height: Int = 1080,
        backgroundType: String = "White"
    ): Long {
        val projectId = idGenerator.getAndIncrement()
        val newProject = ProjectEntity(
            id = projectId,
            name = name,
            fps = fps,
            width = width,
            height = height,
            backgroundType = backgroundType,
            isSynced = false
        )
        projectsFlow.value = projectsFlow.value + newProject

        // Create 3 layers
        insertLayer(
            LayerEntity(
                projectId = projectId,
                name = "Background",
                layerOrder = 0,
                opacity = 0.8f
            )
        )
        insertLayer(
            LayerEntity(
                projectId = projectId,
                name = "Layer 1",
                layerOrder = 1,
                opacity = 1.0f
            )
        )
        insertLayer(
            LayerEntity(
                projectId = projectId,
                name = "Foreground",
                layerOrder = 2,
                opacity = 1.0f
            )
        )

        // Create first frame
        insertFrame(
            FrameEntity(
                projectId = projectId,
                sequenceIndex = 0
            )
        )

        return projectId
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectsFlow.value = projectsFlow.value.map {
            if (it.id == project.id) project else it
        }
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectsFlow.value = projectsFlow.value.filter { it.id != project.id }
        // Cascade delete layers and frames and paths
        val layerIds = layersFlow.value.filter { it.projectId == project.id }.map { it.id }
        layersFlow.value = layersFlow.value.filter { it.projectId != project.id }
        val frameIds = framesFlow.value.filter { it.projectId == project.id }.map { it.id }
        framesFlow.value = framesFlow.value.filter { it.projectId != project.id }
        pathsFlow.value = pathsFlow.value.filter { it.frameId !in frameIds && it.layerId !in layerIds }
    }

    suspend fun insertLayer(layer: LayerEntity): Long {
        val layerId = idGenerator.getAndIncrement()
        val newLayer = layer.copy(id = layerId)
        layersFlow.value = layersFlow.value + newLayer
        return layerId
    }

    suspend fun updateLayer(layer: LayerEntity) {
        layersFlow.value = layersFlow.value.map {
            if (it.id == layer.id) layer else it
        }
    }

    suspend fun deleteLayer(layer: LayerEntity) {
        layersFlow.value = layersFlow.value.filter { it.id != layer.id }
        pathsFlow.value = pathsFlow.value.filter { it.layerId != layer.id }
    }

    suspend fun insertFrame(frame: FrameEntity): Long {
        val frameId = idGenerator.getAndIncrement()
        val newFrame = frame.copy(id = frameId)
        framesFlow.value = framesFlow.value + newFrame
        return frameId
    }

    suspend fun updateFrame(frame: FrameEntity) {
        framesFlow.value = framesFlow.value.map {
            if (it.id == frame.id) frame else it
        }
    }

    suspend fun deleteFrame(frame: FrameEntity) {
        framesFlow.value = framesFlow.value.filter { it.id != frame.id }
        pathsFlow.value = pathsFlow.value.filter { it.frameId != frame.id }
    }

    suspend fun insertDrawPath(drawPath: DrawPathEntity): Long {
        val pathId = idGenerator.getAndIncrement()
        val newPath = drawPath.copy(id = pathId)
        pathsFlow.value = pathsFlow.value + newPath
        return pathId
    }

    suspend fun deleteDrawPathById(pathId: Long) {
        pathsFlow.value = pathsFlow.value.filter { it.id != pathId }
    }

    suspend fun deletePathsForFrameAndLayer(frameId: Long, layerId: Long) {
        pathsFlow.value = pathsFlow.value.filterNot { it.frameId == frameId && it.layerId == layerId }
    }

    suspend fun deletePathsForFrame(frameId: Long) {
        pathsFlow.value = pathsFlow.value.filter { it.frameId != frameId }
    }
}
