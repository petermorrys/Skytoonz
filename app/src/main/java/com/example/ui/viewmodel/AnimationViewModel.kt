package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DrawPathEntity
import com.example.data.model.FrameEntity
import com.example.data.model.LayerEntity
import com.example.data.model.ProjectEntity
import com.example.data.repository.AnimationRepository
import com.example.ui.drawing.BrushConfig
import com.example.ui.drawing.BrushType
import com.example.util.CanvasPoint
import com.example.util.PathSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AnimationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AnimationRepository
    val allProjects: StateFlow<List<ProjectEntity>>

    // Theme Preference State: "System", "Light", "Dark"
    private val sharedPrefs = application.getSharedPreferences("art_animator_prefs", android.content.Context.MODE_PRIVATE)
    private val _themePreference = MutableStateFlow(sharedPrefs.getString("theme_mode", "System") ?: "System")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    fun setThemePreference(preference: String) {
        sharedPrefs.edit().putString("theme_mode", preference).apply()
        _themePreference.value = preference
    }

    // Ruler States
    private val _rulerEnabled = MutableStateFlow(false)
    val rulerEnabled = _rulerEnabled.asStateFlow()

    private val _selectedRuler = MutableStateFlow("LINE") // "LINE", "CIRC", "BOX", "MIRR"
    val selectedRuler = _selectedRuler.asStateFlow()

    private val _rulerLocked = MutableStateFlow(false)
    val rulerLocked = _rulerLocked.asStateFlow()

    private val _rulerCenter = MutableStateFlow(CanvasPoint(540f, 540f, 1f))
    val rulerCenter = _rulerCenter.asStateFlow()

    private val _rulerRadius = MutableStateFlow(250f)
    val rulerRadius = _rulerRadius.asStateFlow()

    fun setRulerEnabled(enabled: Boolean) {
        _rulerEnabled.value = enabled
    }

    fun setSelectedRuler(ruler: String) {
        _selectedRuler.value = ruler
    }

    fun setRulerLocked(locked: Boolean) {
        _rulerLocked.value = locked
    }

    fun setRulerCenter(x: Float, y: Float) {
        if (!_rulerLocked.value) {
            _rulerCenter.value = CanvasPoint(x, y, 1f)
        }
    }

    fun setRulerRadius(r: Float) {
        if (!_rulerLocked.value) {
            _rulerRadius.value = r.coerceAtLeast(20f)
        }
    }

    // Active Project Selection
    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId = _currentProjectId.asStateFlow()

    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject = _currentProject.asStateFlow()

    // Timeline and Playback
    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex = _currentFrameIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // Layers
    private val _layers = MutableStateFlow<List<LayerEntity>>(emptyList())
    val layers = _layers.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<Long?>(null)
    val selectedLayerId = _selectedLayerId.asStateFlow()

    // Frames
    private val _frames = MutableStateFlow<List<FrameEntity>>(emptyList())
    val frames = _frames.asStateFlow()

    // Paths map: frameId -> List of DrawPathEntity
    private val _pathsMap = MutableStateFlow<Map<Long, List<DrawPathEntity>>>(emptyMap())
    val pathsMap = _pathsMap.asStateFlow()

    // Onion Skinning Settings
    private val _onionSkinBefore = MutableStateFlow(true)
    val onionSkinBefore = _onionSkinBefore.asStateFlow()

    private val _onionSkinAfter = MutableStateFlow(true)
    val onionSkinAfter = _onionSkinAfter.asStateFlow()

    private val _onionSkinOpacity = MutableStateFlow(0.4f)
    val onionSkinOpacity = _onionSkinOpacity.asStateFlow()

    // Drawing Canvas State (Current path being drawn)
    private val _currentDrawingPoints = MutableStateFlow<List<CanvasPoint>>(emptyList())
    val currentDrawingPoints = _currentDrawingPoints.asStateFlow()

    // Brush Configurations
    private val _brushConfig = MutableStateFlow(BrushConfig())
    val brushConfig = _brushConfig.asStateFlow()

    // Stylus / Pressure Options
    private val _stylusPressureMultiplier = MutableStateFlow(1.5f)
    val stylusPressureMultiplier = _stylusPressureMultiplier.asStateFlow()

    private val _isPressureSensitivityEnabled = MutableStateFlow(true)
    val isPressureSensitivityEnabled = _isPressureSensitivityEnabled.asStateFlow()

    // Audio Import / Recorder State
    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio = _isRecordingAudio.asStateFlow()

    private val _audioPlaybackProgress = MutableStateFlow(0f)
    val audioPlaybackProgress = _audioPlaybackProgress.asStateFlow()

    // Sync & Export states
    private val _syncState = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val syncState = _syncState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportProgress>(ExportProgress.Idle)
    val exportState = _exportState.asStateFlow()

    // Undo / Redo Stacks (List of draw paths deleted for undo)
    private val undoStack = mutableListOf<DrawPathEntity>()
    private val redoStack = mutableListOf<DrawPathEntity>()

    // Local file path for audio recorder
    private var mediaRecorder: MediaRecorder? = null
    private var recordedAudioFile: File? = null

    // Playback loop job
    private var playbackJob: Job? = null
    private var audioPlayer: MediaPlayer? = null

    init {
        repository = AnimationRepository()

        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Observe active project changes and update related lists
        viewModelScope.launch {
            _currentProjectId.collect { id ->
                if (id != null) {
                    loadProjectDetails(id)
                } else {
                    _currentProject.value = null
                    _frames.value = emptyList()
                    _layers.value = emptyList()
                    _selectedLayerId.value = null
                    _pathsMap.value = emptyMap()
                    _currentFrameIndex.value = 0
                }
            }
        }
    }

    private fun loadProjectDetails(projectId: Long) {
        viewModelScope.launch {
            // Fetch project metadata
            val proj = repository.getProjectById(projectId)
            _currentProject.value = proj

            // Setup listeners for layers, frames
            launch {
                repository.getLayersForProjectFlow(projectId).collect { layersList ->
                    _layers.value = layersList
                    if (_selectedLayerId.value == null || layersList.none { it.id == _selectedLayerId.value }) {
                        // Default to the last layer (Foreground/Layer 1)
                        _selectedLayerId.value = layersList.lastOrNull()?.id
                    }
                }
            }

            launch {
                repository.getFramesForProjectFlow(projectId).collect { framesList ->
                    _frames.value = framesList
                    if (_currentFrameIndex.value >= framesList.size) {
                        _currentFrameIndex.value = (framesList.size - 1).coerceAtLeast(0)
                    }

                    // Dynamically observe and fetch paths for each frame in the project
                    val currentPaths = mutableMapOf<Long, List<DrawPathEntity>>()
                    framesList.forEach { frame ->
                        val paths = repository.getPathsForFrame(frame.id)
                        currentPaths[frame.id] = paths
                    }
                    _pathsMap.value = currentPaths
                }
            }
        }
    }

    fun refreshPaths() {
        val projectId = _currentProjectId.value ?: return
        viewModelScope.launch {
            val framesList = repository.getFramesForProject(projectId)
            val currentPaths = mutableMapOf<Long, List<DrawPathEntity>>()
            framesList.forEach { frame ->
                val paths = repository.getPathsForFrame(frame.id)
                currentPaths[frame.id] = paths
            }
            _pathsMap.value = currentPaths
        }
    }

    // --- Action Methods ---

    fun selectProject(projectId: Long?) {
        _isPlaying.value = false
        playbackJob?.cancel()
        _currentProjectId.value = projectId
        _currentFrameIndex.value = 0
    }

    fun createNewProject(name: String, fps: Int, width: Int = 1080, height: Int = 1080, backgroundType: String = "White") {
        viewModelScope.launch {
            val cleanName = if (name.isBlank()) "Untitled Animation" else name
            val id = repository.createProject(cleanName, fps, width, height, backgroundType)
            selectProject(id)
        }
    }

    fun deleteCurrentProject() {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            repository.deleteProject(proj)
            selectProject(null)
        }
    }

    // --- Brush Configuration ---
    fun updateBrushColor(color: Color) {
        _brushConfig.value = _brushConfig.value.copy(color = color)
    }

    fun updateBrushSize(size: Float) {
        _brushConfig.value = _brushConfig.value.copy(size = size)
    }

    fun updateBrushOpacity(opacity: Float) {
        _brushConfig.value = _brushConfig.value.copy(opacity = opacity)
    }

    fun updateBrushType(type: BrushType) {
        _brushConfig.value = _brushConfig.value.copy(type = type)
    }

    fun setPressureSensitivityEnabled(enabled: Boolean) {
        _isPressureSensitivityEnabled.value = enabled
    }

    fun updateStylusPressureMultiplier(multiplier: Float) {
        _stylusPressureMultiplier.value = multiplier
    }

    fun setOnionSkinBeforeEnabled(enabled: Boolean) {
        _onionSkinBefore.value = enabled
    }

    fun setOnionSkinAfterEnabled(enabled: Boolean) {
        _onionSkinAfter.value = enabled
    }

    fun setOnionSkinOpacity(opacity: Float) {
        _onionSkinOpacity.value = opacity
    }

    // --- Timeline / Playback ---
    fun selectFrameIndex(index: Int) {
        val size = _frames.value.size
        if (size > 0) {
            _currentFrameIndex.value = index.coerceIn(0, size - 1)
        }
    }

    fun nextFrame() {
        val size = _frames.value.size
        if (size > 0) {
            _currentFrameIndex.value = (_currentFrameIndex.value + 1) % size
        }
    }

    fun prevFrame() {
        val size = _frames.value.size
        if (size > 0) {
            _currentFrameIndex.value = (_currentFrameIndex.value - 1 + size) % size
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        val project = _currentProject.value ?: return
        val framesList = _frames.value
        if (framesList.isEmpty()) return

        _isPlaying.value = true
        val delayMillis = (1000L / project.fps).coerceAtLeast(16L)

        // Setup audio playback if loaded
        startAudioPlaybackIfAvailable()

        playbackJob = viewModelScope.launch {
            while (_isPlaying.value) {
                nextFrame()
                delay(delayMillis)
            }
        }
    }

    private fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        stopAudioPlayback()
    }

    // --- Frame Manipulation ---
    fun addNewFrame() {
        val projectId = _currentProjectId.value ?: return
        val framesList = _frames.value
        viewModelScope.launch {
            val nextIndex = framesList.size
            val frame = FrameEntity(projectId = projectId, sequenceIndex = nextIndex)
            repository.insertFrame(frame)
            _currentFrameIndex.value = nextIndex
        }
    }

    fun duplicateCurrentFrame() {
        val projectId = _currentProjectId.value ?: return
        val framesList = _frames.value
        val currentIndex = _currentFrameIndex.value
        if (framesList.isEmpty()) return

        viewModelScope.launch {
            val sourceFrame = framesList[currentIndex]
            val nextIndex = currentIndex + 1

            // Shift indices of subsequent frames
            for (i in framesList.size - 1 downTo nextIndex) {
                val f = framesList[i]
                repository.updateFrame(f.copy(sequenceIndex = f.sequenceIndex + 1))
            }

            // Insert new duplicate frame
            val newFrameId = repository.insertFrame(FrameEntity(projectId = projectId, sequenceIndex = nextIndex))

            // Copy all paths from sourceFrame to newFrame
            val sourcePaths = repository.getPathsForFrame(sourceFrame.id)
            sourcePaths.forEach { path ->
                repository.insertDrawPath(
                    DrawPathEntity(
                        frameId = newFrameId,
                        layerId = path.layerId,
                        color = path.color,
                        strokeWidth = path.strokeWidth,
                        brushType = path.brushType,
                        pointsData = path.pointsData
                    )
                )
            }

            _currentFrameIndex.value = nextIndex
            refreshPaths()
        }
    }

    // Clipboard for copying and pasting strokes
    private var copiedPaths: List<DrawPathEntity> = emptyList()

    fun copyCurrentFrameLayerPaths() {
        val frameId = getCurrentFrameId() ?: return
        val layerId = _selectedLayerId.value ?: return
        val paths = pathsMap.value[frameId]?.filter { it.layerId == layerId } ?: emptyList()
        copiedPaths = paths
    }

    fun pasteFrameLayerPaths() {
        val frameId = getCurrentFrameId() ?: return
        val layerId = _selectedLayerId.value ?: return
        if (copiedPaths.isEmpty()) return

        viewModelScope.launch {
            copiedPaths.forEach { path ->
                repository.insertDrawPath(
                    DrawPathEntity(
                        frameId = frameId,
                        layerId = layerId,
                        color = path.color,
                        strokeWidth = path.strokeWidth,
                        brushType = path.brushType,
                        pointsData = path.pointsData
                    )
                )
            }
            refreshPaths()
        }
    }

    fun deleteCurrentFrame() {
        val framesList = _frames.value
        val currentIndex = _currentFrameIndex.value
        if (framesList.size <= 1) return // Keep at least one frame

        viewModelScope.launch {
            val frameToDelete = framesList[currentIndex]
            repository.deleteFrame(frameToDelete)

            // Shift subsequent frame indices
            for (i in currentIndex + 1 until framesList.size) {
                val f = framesList[i]
                repository.updateFrame(f.copy(sequenceIndex = f.sequenceIndex - 1))
            }

            _currentFrameIndex.value = (currentIndex - 1).coerceAtLeast(0)
            refreshPaths()
        }
    }

    fun moveFrame(fromIndex: Int, toIndex: Int) {
        val framesList = _frames.value
        if (fromIndex !in framesList.indices || toIndex !in framesList.indices) return

        viewModelScope.launch {
            val fromFrame = framesList[fromIndex]
            val toFrame = framesList[toIndex]

            repository.updateFrame(fromFrame.copy(sequenceIndex = toIndex))
            repository.updateFrame(toFrame.copy(sequenceIndex = fromIndex))

            _currentFrameIndex.value = toIndex
            refreshPaths()
        }
    }

    // --- Layers Manipulation ---
    fun selectLayer(layerId: Long) {
        _selectedLayerId.value = layerId
    }

    fun toggleLayerVisibility(layer: LayerEntity) {
        viewModelScope.launch {
            repository.updateLayer(layer.copy(isVisible = !layer.isVisible))
        }
    }

    fun updateLayerOpacity(layer: LayerEntity, opacity: Float) {
        viewModelScope.launch {
            repository.updateLayer(layer.copy(opacity = opacity))
        }
    }

    fun addNewLayer(name: String) {
        val projectId = _currentProjectId.value ?: return
        val order = _layers.value.size
        viewModelScope.launch {
            val cleanName = if (name.isBlank()) "Layer ${order + 1}" else name
            val layerId = repository.insertLayer(
                LayerEntity(projectId = projectId, name = cleanName, layerOrder = order)
            )
            _selectedLayerId.value = layerId
        }
    }

    // --- Drawing Engine Callbacks ---
    fun startDrawingPath(x: Float, y: Float, pressure: Float) {
        if (_isPlaying.value) return
        val actualPressure = if (_isPressureSensitivityEnabled.value) pressure else 1.0f

        val finalPoint = if (_rulerEnabled.value) {
            when (_selectedRuler.value) {
                "CIRC" -> {
                    val cx = _rulerCenter.value.x
                    val cy = _rulerCenter.value.y
                    val r = _rulerRadius.value
                    val dx = x - cx
                    val dy = y - cy
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val snappedX = if (dist > 0) cx + dx * (r / dist) else cx + r
                    val snappedY = if (dist > 0) cy + dy * (r / dist) else cy
                    CanvasPoint(snappedX, snappedY, actualPressure)
                }
                "BOX" -> {
                    val cx = _rulerCenter.value.x
                    val cy = _rulerCenter.value.y
                    val hw = _rulerRadius.value
                    val clampedX = x.coerceIn(cx - hw, cx + hw)
                    val clampedY = y.coerceIn(cy - hw, cy + hw)
                    val distToLeft = Math.abs(x - (cx - hw))
                    val distToRight = Math.abs(x - (cx + hw))
                    val distToTop = Math.abs(y - (cy - hw))
                    val distToBottom = Math.abs(y - (cy + hw))
                    val minDist = listOf(distToLeft, distToRight, distToTop, distToBottom).minOrNull() ?: 0f
                    val snappedX: Float
                    val snappedY: Float
                    when (minDist) {
                        distToLeft -> {
                            snappedX = cx - hw
                            snappedY = clampedY
                        }
                        distToRight -> {
                            snappedX = cx + hw
                            snappedY = clampedY
                        }
                        distToTop -> {
                            snappedX = clampedX
                            snappedY = cy - hw
                        }
                        else -> {
                            snappedX = clampedX
                            snappedY = cy + hw
                        }
                    }
                    CanvasPoint(snappedX, snappedY, actualPressure)
                }
                else -> CanvasPoint(x, y, actualPressure)
            }
        } else {
            CanvasPoint(x, y, actualPressure)
        }

        _currentDrawingPoints.value = listOf(finalPoint)
    }

    fun addPointToDrawingPath(x: Float, y: Float, pressure: Float) {
        if (_isPlaying.value) return
        val actualPressure = if (_isPressureSensitivityEnabled.value) pressure else 1.0f

        if (_rulerEnabled.value) {
            when (_selectedRuler.value) {
                "LINE" -> {
                    val firstPoint = _currentDrawingPoints.value.firstOrNull() ?: CanvasPoint(x, y, actualPressure)
                    _currentDrawingPoints.value = listOf(firstPoint, CanvasPoint(x, y, actualPressure))
                }
                "CIRC" -> {
                    val cx = _rulerCenter.value.x
                    val cy = _rulerCenter.value.y
                    val r = _rulerRadius.value
                    val dx = x - cx
                    val dy = y - cy
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val snappedX = if (dist > 0) cx + dx * (r / dist) else cx + r
                    val snappedY = if (dist > 0) cy + dy * (r / dist) else cy
                    val snappedPoint = CanvasPoint(snappedX, snappedY, actualPressure)
                    _currentDrawingPoints.value = _currentDrawingPoints.value + snappedPoint
                }
                "BOX" -> {
                    val cx = _rulerCenter.value.x
                    val cy = _rulerCenter.value.y
                    val hw = _rulerRadius.value
                    val clampedX = x.coerceIn(cx - hw, cx + hw)
                    val clampedY = y.coerceIn(cy - hw, cy + hw)
                    val distToLeft = Math.abs(x - (cx - hw))
                    val distToRight = Math.abs(x - (cx + hw))
                    val distToTop = Math.abs(y - (cy - hw))
                    val distToBottom = Math.abs(y - (cy + hw))
                    val minDist = listOf(distToLeft, distToRight, distToTop, distToBottom).minOrNull() ?: 0f
                    val snappedX: Float
                    val snappedY: Float
                    when (minDist) {
                        distToLeft -> {
                            snappedX = cx - hw
                            snappedY = clampedY
                        }
                        distToRight -> {
                            snappedX = cx + hw
                            snappedY = clampedY
                        }
                        distToTop -> {
                            snappedX = clampedX
                            snappedY = cy - hw
                        }
                        else -> {
                            snappedX = clampedX
                            snappedY = cy + hw
                        }
                    }
                    val snappedPoint = CanvasPoint(snappedX, snappedY, actualPressure)
                    _currentDrawingPoints.value = _currentDrawingPoints.value + snappedPoint
                }
                "MIRR" -> {
                    val newPoint = CanvasPoint(x, y, actualPressure)
                    _currentDrawingPoints.value = _currentDrawingPoints.value + newPoint
                }
            }
        } else {
            val newPoint = CanvasPoint(x, y, actualPressure)
            _currentDrawingPoints.value = _currentDrawingPoints.value + newPoint
        }
    }

    fun endDrawingPath() {
        if (_isPlaying.value) return
        val points = _currentDrawingPoints.value
        if (points.isEmpty()) return

        val frameId = getCurrentFrameId() ?: return
        val layerId = _selectedLayerId.value ?: return
        val brush = _brushConfig.value

        viewModelScope.launch {
            val pointsStr = PathSerializer.serialize(points)
            val pathEntity = DrawPathEntity(
                frameId = frameId,
                layerId = layerId,
                color = brush.color.hashCode(),
                strokeWidth = brush.size,
                brushType = brush.type.name,
                pointsData = pointsStr
            )

            // Save to DB
            repository.insertDrawPath(pathEntity)

            // If MIRR is active, save a mirrored copy of the stroke as a separate path!
            if (_rulerEnabled.value && _selectedRuler.value == "MIRR") {
                val proj = _currentProject.value
                val projectWidth = proj?.width ?: 1080
                val mirroredPoints = points.map { CanvasPoint(projectWidth - it.x, it.y, it.pressure) }
                val mirroredPointsStr = PathSerializer.serialize(mirroredPoints)
                val mirroredPathEntity = pathEntity.copy(pointsData = mirroredPointsStr)
                repository.insertDrawPath(mirroredPathEntity)
            }

            // Immediately load paths for the active frame to prevent flicker
            val updatedActivePaths = repository.getPathsForFrame(frameId)
            val newPathsMap = _pathsMap.value.toMutableMap()
            newPathsMap[frameId] = updatedActivePaths
            _pathsMap.value = newPathsMap

            // Now safely reset live drawing points
            _currentDrawingPoints.value = emptyList()

            // Update state & clear redo
            redoStack.clear()
            refreshPaths()
        }
    }

    fun clearActiveFrameAndLayer() {
        val frameId = getCurrentFrameId() ?: return
        val layerId = _selectedLayerId.value ?: return
        viewModelScope.launch {
            repository.deletePathsForFrameAndLayer(frameId, layerId)
            
            // Immediately clear locally to reflect change instantly
            val newPathsMap = _pathsMap.value.toMutableMap()
            newPathsMap[frameId] = emptyList()
            _pathsMap.value = newPathsMap
            
            refreshPaths()
        }
    }

    // --- Undo & Redo ---
    fun undo() {
        val frameId = getCurrentFrameId() ?: return
        val layerId = _selectedLayerId.value ?: return
        viewModelScope.launch {
            val paths = repository.getPathsForFrame(frameId).filter { it.layerId == layerId }
            if (paths.isNotEmpty()) {
                val lastPath = paths.last()
                repository.deleteDrawPathById(lastPath.id)
                redoStack.add(lastPath)
                
                // Immediately refresh active frame locally to reflect change instantly
                val updatedPaths = repository.getPathsForFrame(frameId)
                val newPathsMap = _pathsMap.value.toMutableMap()
                newPathsMap[frameId] = updatedPaths
                _pathsMap.value = newPathsMap
                
                refreshPaths()
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val pathToRestore = redoStack.removeAt(redoStack.size - 1)
            viewModelScope.launch {
                repository.insertDrawPath(pathToRestore)
                val frameId = pathToRestore.frameId
                
                // Immediately refresh active frame locally to reflect change instantly
                val updatedPaths = repository.getPathsForFrame(frameId)
                val newPathsMap = _pathsMap.value.toMutableMap()
                newPathsMap[frameId] = updatedPaths
                _pathsMap.value = newPathsMap
                
                refreshPaths()
            }
        }
    }

    // --- Helper Getters ---
    fun getCurrentFrameId(): Long? {
        val framesList = _frames.value
        val index = _currentFrameIndex.value
        return if (index in framesList.indices) framesList[index].id else null
    }

    fun getOnionSkinBeforePaths(): List<DrawPathEntity> {
        val index = _currentFrameIndex.value
        val framesList = _frames.value
        if (index > 0 && index - 1 in framesList.indices && _onionSkinBefore.value) {
            val prevFrameId = framesList[index - 1].id
            return _pathsMap.value[prevFrameId] ?: emptyList()
        }
        return emptyList()
    }

    fun getOnionSkinAfterPaths(): List<DrawPathEntity> {
        val index = _currentFrameIndex.value
        val framesList = _frames.value
        if (index + 1 in framesList.indices && _onionSkinAfter.value) {
            val nextFrameId = framesList[index + 1].id
            return _pathsMap.value[nextFrameId] ?: emptyList()
        }
        return emptyList()
    }

    // --- Cloud Sync ---
    fun toggleCloudSync() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            _syncState.value = SyncProgress.Syncing(0.1f)
            delay(400)
            _syncState.value = SyncProgress.Syncing(0.4f)
            delay(500)
            _syncState.value = SyncProgress.Syncing(0.8f)
            delay(400)

            val updatedProj = project.copy(isSynced = !project.isSynced)
            repository.updateProject(updatedProj)
            _currentProject.value = updatedProj

            _syncState.value = SyncProgress.Success
            delay(2000)
            _syncState.value = SyncProgress.Idle
        }
    }

    // --- Media / Exports ---
    fun exportProject(exportType: String) {
        viewModelScope.launch {
            _exportState.value = ExportProgress.Exporting("Rendering $exportType frames...", 0.1f)
            delay(600)
            _exportState.value = ExportProgress.Exporting("Compiling layers...", 0.4f)
            delay(600)
            _exportState.value = ExportProgress.Exporting("Applying audio tracks...", 0.7f)
            delay(600)
            _exportState.value = ExportProgress.Exporting("Publishing social-media package...", 0.9f)
            delay(400)
            _exportState.value = ExportProgress.Success("Project successfully exported as $exportType!")
        }
    }

    fun dismissExportState() {
        _exportState.value = ExportProgress.Idle
    }

    // --- Audio Import & Mic Recording ---
    fun startRecordingAudio() {
        try {
            val context = getApplication<Application>().applicationContext
            recordedAudioFile = File(context.cacheDir, "audio_recording_${System.currentTimeMillis()}.3gp")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(recordedAudioFile?.absolutePath)
                prepare()
                start()
            }
            _isRecordingAudio.value = true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start audio recording", e)
        }
    }

    fun stopRecordingAudio() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecordingAudio.value = false

            // Link the recording to the project
            val project = _currentProject.value
            val file = recordedAudioFile
            if (project != null && file != null && file.exists()) {
                val updatedProj = project.copy(
                    audioTrackName = "Mic Recording - ${file.name.substringAfterLast("_").substringBefore(".")}",
                    audioTrackDurationMs = 5000L // Simulated duration for mic recording
                )
                viewModelScope.launch {
                    repository.updateProject(updatedProj)
                    _currentProject.value = updatedProj
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording cleanly", e)
        }
    }

    fun selectPresetAudio(presetName: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val updated = project.copy(
                audioTrackName = presetName,
                audioTrackDurationMs = 8000L
            )
            repository.updateProject(updated)
            _currentProject.value = updated
        }
    }

    fun clearAudioTrack() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val updated = project.copy(
                audioTrackName = null,
                audioTrackDurationMs = 0L
            )
            repository.updateProject(updated)
            _currentProject.value = updated
        }
    }

    private fun startAudioPlaybackIfAvailable() {
        val project = _currentProject.value ?: return
        if (project.audioTrackName != null) {
            // Simulate playing sound track via system beeper or dynamic track progress
            viewModelScope.launch {
                _audioPlaybackProgress.value = 0f
                while (_isPlaying.value) {
                    val progress = _audioPlaybackProgress.value + 0.05f
                    _audioPlaybackProgress.value = if (progress >= 1f) 0f else progress
                    delay(100)
                }
            }
        }
    }

    private fun stopAudioPlayback() {
        _audioPlaybackProgress.value = 0f
    }

    // --- Keyboard Shortcut Helpers ---
    fun handleKeyboardShortcut(keyChar: Char): Boolean {
        return when (keyChar.lowercaseChar()) {
            ' ' -> { // Space: play/pause
                togglePlayback()
                true
            }
            'n' -> { // N: next frame
                nextFrame()
                true
            }
            'p' -> { // P: previous frame
                prevFrame()
                true
            }
            'z' -> { // Z: Undo
                undo()
                true
            }
            'y' -> { // Y: Redo
                redo()
                true
            }
            'b' -> { // B: Brush (switch to Pen/Brush)
                updateBrushType(BrushType.BRUSH)
                true
            }
            'e' -> { // E: Eraser
                updateBrushType(BrushType.ERASER)
                true
            }
            'a' -> { // A: Add Frame
                addNewFrame()
                true
            }
            'd' -> { // D: Duplicate Frame
                duplicateCurrentFrame()
                true
            }
            else -> false
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        mediaRecorder?.release()
        audioPlayer?.release()
    }
}

sealed class SyncProgress {
    object Idle : SyncProgress()
    data class Syncing(val progress: Float) : SyncProgress()
    object Success : SyncProgress()
}

sealed class ExportProgress {
    object Idle : ExportProgress()
    data class Exporting(val taskName: String, val progress: Float) : ExportProgress()
    data class Success(val message: String) : ExportProgress()
}
