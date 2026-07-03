package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LayerEntity
import com.example.ui.drawing.BrushType
import com.example.ui.viewmodel.AnimationViewModel
import com.example.ui.viewmodel.ExportProgress
import com.example.ui.viewmodel.SyncProgress
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorWorkspace(
    viewModel: AnimationViewModel,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val currentFrameIndex by viewModel.currentFrameIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val frames by viewModel.frames.collectAsState()
    val layers by viewModel.layers.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val brushConfig by viewModel.brushConfig.collectAsState()

    // Control Dialog/Drawer expansion
    var activePanel by remember { mutableStateOf<ActivePanel?>(null) }
    val scope = rememberCoroutineScope()

    // Sync state
    val syncState by viewModel.syncState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    // Swatches for color selection
    val colorSwatches = listOf(
        Color.Black, Color.DarkGray, Color.LightGray, Color.White,
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFBBF24), Color(0xFF10B981),
        Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF8B5CF6),
        Color(0xFFEC4899)
    )

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF6366F1))
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2FA)) // Bento Background
            .onKeyEvent { keyEvent ->
                // Capture physical keyboard short-cuts
                val codePoint = keyEvent.nativeKeyEvent.unicodeChar
                if (codePoint > 0) {
                    viewModel.handleKeyboardShortcut(codePoint.toChar())
                } else {
                    false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- TOP HEADER ACTION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Dashboard", tint = Color(0xFF1D1B20))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = project?.name ?: "Drawing Room",
                            color = Color(0xFF1D1B20),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Frame ${currentFrameIndex + 1} of ${frames.size}",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Undo
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color(0xFF1D1B20))
                    }
                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color(0xFF1D1B20))
                    }
                    // Sync Status
                    IconButton(onClick = { viewModel.toggleCloudSync() }) {
                        val icon = if (project?.isSynced == true) Icons.Outlined.CloudDone else Icons.Outlined.CloudQueue
                        val tint = if (project?.isSynced == true) Color(0xFF22C55E) else Color(0xFF1D1B20)
                        Icon(icon, contentDescription = "Cloud Sync", tint = tint)
                    }
                    // Settings Drawer Trigger
                    IconButton(onClick = { activePanel = if (activePanel == ActivePanel.SETTINGS) null else ActivePanel.SETTINGS }) {
                        Icon(Icons.Default.Settings, contentDescription = "Canvas Settings", tint = Color(0xFF1D1B20))
                    }
                    // Share/Export Trigger
                    IconButton(onClick = { activePanel = if (activePanel == ActivePanel.EXPORT) null else ActivePanel.EXPORT }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Animation", tint = Color(0xFF1D1B20))
                    }
                }
            }

            // --- MAIN INTERACTIVE WORKSPACE (Canvas) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                DrawingCanvas(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                // Quick Brush Controls Overlay on Left (Bento Styled)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(12.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Brush Selector Trigger
                    IconButton(
                        onClick = { activePanel = if (activePanel == ActivePanel.BRUSH) null else ActivePanel.BRUSH },
                        modifier = Modifier.background(
                            if (activePanel == ActivePanel.BRUSH) Color(0xFFEADDFF) else Color.Transparent,
                            CircleShape
                        )
                    ) {
                        val icon = when (brushConfig.type) {
                            BrushType.PEN -> Icons.Default.Brush
                            BrushType.PENCIL -> Icons.Default.Create
                            BrushType.BRUSH -> Icons.Default.Gesture
                            BrushType.AIRBRUSH -> Icons.Default.Grain
                            BrushType.ERASER -> Icons.Default.AutoFixNormal
                        }
                        val tint = if (activePanel == ActivePanel.BRUSH) Color(0xFF21005D) else Color(0xFF49454F)
                        Icon(icon, contentDescription = "Brush Type", tint = tint)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Layers Trigger
                    IconButton(
                        onClick = { activePanel = if (activePanel == ActivePanel.LAYERS) null else ActivePanel.LAYERS },
                        modifier = Modifier.background(
                            if (activePanel == ActivePanel.LAYERS) Color(0xFFE8DEF8) else Color.Transparent,
                            CircleShape
                        )
                    ) {
                        val tint = if (activePanel == ActivePanel.LAYERS) Color(0xFF1D192B) else Color(0xFF49454F)
                        Icon(Icons.Default.Layers, contentDescription = "Layers Management", tint = tint)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Audio track Trigger
                    IconButton(
                        onClick = { activePanel = if (activePanel == ActivePanel.AUDIO) null else ActivePanel.AUDIO },
                        modifier = Modifier.background(
                            if (project?.audioTrackName != null) Color(0xFFD0BCFF) else Color.Transparent,
                            CircleShape
                        )
                    ) {
                        val icon = if (project?.audioTrackName != null) Icons.Default.MusicNote else Icons.Default.MusicVideo
                        val tint = if (project?.audioTrackName != null) Color(0xFF21005D) else Color(0xFF49454F)
                        Icon(icon, contentDescription = "Audio track sync", tint = tint)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Swatch Color Circular Display
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(brushConfig.color)
                            .border(1.5.dp, Color(0xFFCAC4D0), CircleShape)
                            .clickable { activePanel = ActivePanel.BRUSH }
                    )
                }

                // Audio track playing indicator overlay at top right of canvas
                if (project?.audioTrackName != null) {
                    val audioProgress by viewModel.audioPlaybackProgress.collectAsState()
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color(0xE622C55E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio track playing", tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${project?.audioTrackName} (${(audioProgress * 100).toInt()}%)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BOTTOM TIMELINE & PLAYBACK AREA (Bento Styled Card) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    // Playback Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { viewModel.clearActiveFrameAndLayer() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear frame", tint = Color(0xFFEF4444))
                            }
                            IconButton(onClick = { viewModel.duplicateCurrentFrame() }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Frame", tint = Color(0xFF1D1B20))
                            }
                        }

                        // Main Controls: Prev, Play/Pause, Next
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.prevFrame() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Frame", tint = Color(0xFF1D1B20), modifier = Modifier.size(28.dp))
                            }

                            FloatingActionButton(
                                onClick = { viewModel.togglePlayback() },
                                containerColor = Color(0xFF6750A4), // Bento primary purple
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("play_pause_fab")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.nextFrame() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Frame", tint = Color(0xFF1D1B20), modifier = Modifier.size(28.dp))
                            }
                        }

                        Button(
                            onClick = { viewModel.addNewFrame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADDFF), contentColor = Color(0xFF21005D)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_frame_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Timeline Scroller
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(frames) { index, frame ->
                                val isSelected = index == currentFrameIndex
                                Box(
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF6750A4) else Color(0xFFF3EDF7))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectFrameIndex(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${index + 1}",
                                            color = if (isSelected) Color.White else Color(0xFF1D1B20),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Frame",
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF49454F).copy(alpha = 0.8f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SUBPANELS AND OVERLAYS ---

        // Draw overlay if sync is occurring
        AnimatedVisibility(
            visible = syncState is SyncProgress.Syncing || syncState is SyncProgress.Success,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cloud Synchronization", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (syncState is SyncProgress.Syncing) {
                            LinearProgressIndicator(color = Color(0xFFEC4899), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Securing project vectors...", color = Color.LightGray, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Synced successfully!", color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Export overlay
        AnimatedVisibility(
            visible = exportState is ExportProgress.Exporting || exportState is ExportProgress.Success,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Layered Movie Export", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        when (val state = exportState) {
                            is ExportProgress.Exporting -> {
                                LinearProgressIndicator(progress = state.progress, color = Color(0xFFEC4899), modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.taskName, color = Color.LightGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                            is ExportProgress.Success -> {
                                Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message, color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.dismissExportState() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                ) {
                                    Text("Awesome")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // 1. Brush Configuration Panel
        if (activePanel == ActivePanel.BRUSH) {
            BrushPanel(
                config = brushConfig,
                colorSwatches = colorSwatches,
                onColorSelected = { viewModel.updateBrushColor(it) },
                onSizeChanged = { viewModel.updateBrushSize(it) },
                onOpacityChanged = { viewModel.updateBrushOpacity(it) },
                onTypeSelected = { viewModel.updateBrushType(it) },
                onDismiss = { activePanel = null }
            )
        }

        // 2. Layers Panel
        if (activePanel == ActivePanel.LAYERS) {
            LayersPanel(
                layers = layers,
                selectedId = selectedLayerId,
                onSelectLayer = { viewModel.selectLayer(it) },
                onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                onOpacityChanged = { layer, op -> viewModel.updateLayerOpacity(layer, op) },
                onAddLayer = { viewModel.addNewLayer(it) },
                onDismiss = { activePanel = null }
            )
        }

        // 3. Settings / Onion Skin / Keyboard Shortcuts Panel
        if (activePanel == ActivePanel.SETTINGS) {
            SettingsPanel(
                viewModel = viewModel,
                onDismiss = { activePanel = null }
            )
        }

        // 4. Audio synchronization panel
        if (activePanel == ActivePanel.AUDIO) {
            AudioPanel(
                viewModel = viewModel,
                onDismiss = { activePanel = null }
            )
        }

        // 5. Export Panel
        if (activePanel == ActivePanel.EXPORT) {
            ExportPanel(
                onExport = { type -> viewModel.exportProject(type) },
                onDismiss = { activePanel = null }
            )
        }
    }
}

// Subpanels Enum
enum class ActivePanel {
    BRUSH, LAYERS, SETTINGS, AUDIO, EXPORT
}

@Composable
fun BrushPanel(
    config: com.example.ui.drawing.BrushConfig,
    colorSwatches: List<Color>,
    onColorSelected: (Color) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onTypeSelected: (BrushType) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Brush configurations", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1D1B20))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brush Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BrushType.values().forEach { type ->
                    val isSelected = config.type == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF6750A4) else Color(0xFFF3EDF7))
                            .clickable { onTypeSelected(type) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.label,
                            color = if (isSelected) Color.White else Color(0xFF49454F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stroke Size
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size:", color = Color(0xFF49454F), modifier = Modifier.width(60.dp), fontSize = 13.sp)
                Slider(
                    value = config.size,
                    onValueChange = onSizeChanged,
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6750A4),
                        activeTrackColor = Color(0xFF6750A4),
                        inactiveTrackColor = Color(0xFFF3EDF7)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text("${config.size.toInt()}px", color = Color(0xFF1D1B20), modifier = Modifier.width(48.dp), textAlign = TextAlign.End, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Opacity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opacity:", color = Color(0xFF49454F), modifier = Modifier.width(60.dp), fontSize = 13.sp)
                Slider(
                    value = config.opacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6750A4),
                        activeTrackColor = Color(0xFF6750A4),
                        inactiveTrackColor = Color(0xFFF3EDF7)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text("${(config.opacity * 100).toInt()}%", color = Color(0xFF1D1B20), modifier = Modifier.width(48.dp), textAlign = TextAlign.End, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Swatches
            Text("Colors", color = Color(0xFF49454F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(colorSwatches) { _, color ->
                    val isSelected = config.color == color
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun LayersPanel(
    layers: List<LayerEntity>,
    selectedId: Long?,
    onSelectLayer: (Long) -> Unit,
    onToggleVisibility: (LayerEntity) -> Unit,
    onOpacityChanged: (LayerEntity, Float) -> Unit,
    onAddLayer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newLayerName by remember { mutableStateFlowOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Layers drawer", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1D1B20))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of Layers
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                layers.forEach { layer ->
                    val isSelected = layer.id == selectedId
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectLayer(layer.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Layers else Icons.Outlined.Layers,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = layer.name,
                                        color = Color(0xFF1D1B20),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }

                                IconButton(onClick = { onToggleVisibility(layer) }) {
                                    Icon(
                                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle visibility",
                                        tint = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F)
                                    )
                                }
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Opacity", color = Color(0xFF49454F), fontSize = 11.sp, modifier = Modifier.width(50.dp))
                                    Slider(
                                        value = layer.opacity,
                                        onValueChange = { onOpacityChanged(layer, it) },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF6750A4),
                                            activeTrackColor = Color(0xFF6750A4),
                                            inactiveTrackColor = Color(0xFFD0BCFF)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${(layer.opacity * 100).toInt()}%", color = Color(0xFF1D1B20), fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add new layer quick field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newLayerName,
                    onValueChange = { newLayerName = it },
                    placeholder = { Text("New layer name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedLabelColor = Color(0xFF6750A4)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (newLayerName.isNotBlank()) {
                            onAddLayer(newLayerName)
                            newLayerName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
fun SettingsPanel(
    viewModel: AnimationViewModel,
    onDismiss: () -> Unit
) {
    val onionSkinBefore by viewModel.onionSkinBefore.collectAsState()
    val onionSkinAfter by viewModel.onionSkinAfter.collectAsState()
    val onionSkinOpacity by viewModel.onionSkinOpacity.collectAsState()
    val isPressureSensitivityEnabled by viewModel.isPressureSensitivityEnabled.collectAsState()
    val stylusPressureMultiplier by viewModel.stylusPressureMultiplier.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Canvas & Stylus Settings", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1D1B20))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Onion Skinning options
            Text("Onion skinning parameters", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Previous Frame (Red)", color = Color(0xFF1D1B20), fontSize = 14.sp)
                Switch(
                    checked = onionSkinBefore,
                    onCheckedChange = { viewModel.setOnionSkinBeforeEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6750A4), checkedTrackColor = Color(0xFFD0BCFF))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Next Frame (Green)", color = Color(0xFF1D1B20), fontSize = 14.sp)
                Switch(
                    checked = onionSkinAfter,
                    onCheckedChange = { viewModel.setOnionSkinAfterEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF22C55E), checkedTrackColor = Color(0xFFB5F1CC))
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Onion opacity", color = Color(0xFF49454F), modifier = Modifier.width(110.dp), fontSize = 12.sp)
                Slider(
                    value = onionSkinOpacity,
                    onValueChange = { viewModel.setOnionSkinOpacity(it) },
                    valueRange = 0.1f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6750A4),
                        activeTrackColor = Color(0xFF6750A4),
                        inactiveTrackColor = Color(0xFFF3EDF7)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            androidx.compose.material3.HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))

            // Pressure Sensitivity Support
            Text("Stylus & Touch controls", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stylus Pressure Sensitivity", color = Color(0xFF1D1B20), fontSize = 14.sp)
                Switch(
                    checked = isPressureSensitivityEnabled,
                    onCheckedChange = { viewModel.setPressureSensitivityEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6750A4), checkedTrackColor = Color(0xFFD0BCFF))
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pressure scale", color = Color(0xFF49454F), modifier = Modifier.width(110.dp), fontSize = 12.sp)
                Slider(
                    value = stylusPressureMultiplier,
                    onValueChange = { viewModel.updateStylusPressureMultiplier(it) },
                    valueRange = 0.5f..3f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6750A4),
                        activeTrackColor = Color(0xFF6750A4),
                        inactiveTrackColor = Color(0xFFF3EDF7)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            androidx.compose.material3.HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))

            // Key Shortcuts Info Modal
            Text("Physical Keyboard shortcuts", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShortcutHelpRow("[Space]", "Play / Pause Animation")
                ShortcutHelpRow("[N] / [P]", "Next Frame / Previous Frame")
                ShortcutHelpRow("[Z] / [Y]", "Undo / Redo Vector Stroke")
                ShortcutHelpRow("[B] / [E]", "Select Brush / Select Eraser")
                ShortcutHelpRow("[A] / [D]", "Add Frame / Duplicate Frame")
            }
        }
    }
}

@Composable
fun ShortcutHelpRow(key: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(desc, color = Color(0xFF49454F), fontSize = 12.sp)
    }
}

@Composable
fun AudioPanel(
    viewModel: AnimationViewModel,
    onDismiss: () -> Unit
) {
    val project by viewModel.currentProject.collectAsState()
    val isRecording by viewModel.isRecordingAudio.collectAsState()

    val presetTracks = listOf(
        "8-bit Retro Beat", "Electric Synth Loop", "Whimsical Woodwinds",
        "Lo-fi Cozy Chords", "Cyberpunk Techno Pulse", "Nature Wind Ambience"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Audio Track Synchronization", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1D1B20))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sound Recorder panel (Bento Styled Card)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Mic Recorder", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (isRecording) "Recording live voice/sound effects..." else "Add customized audio via microphone",
                            color = if (isRecording) Color(0xFFEF4444) else Color(0xFF49454F),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecordingAudio()
                            } else {
                                viewModel.startRecordingAudio()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFEF4444) else Color(0xFF6750A4)
                        )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRecording) "Stop" else "Record")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Loops
            Text("Soundtrack presets library", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                presetTracks.forEach { track ->
                    val isSelected = project?.audioTrackName == track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7))
                            .border(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectPresetAudio(track) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(track, color = Color(0xFF1D1B20), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }

                        if (isSelected) {
                            Text("Active", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (project?.audioTrackName != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.clearAudioTrack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Current Sound Track")
                }
            }
        }
    }
}

@Composable
fun ExportPanel(
    onExport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Publish & Share Animation", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1D1B20))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Export as GIF
                ExportFormatCard(
                    title = "Animated GIF",
                    desc = "Ideal for quick social posts & web loop shares",
                    icon = Icons.Default.Gif,
                    color = Color(0xFF6750A4),
                    onClick = { onExport("GIF") },
                    modifier = Modifier.weight(1f)
                )

                // Export as Video MP4
                ExportFormatCard(
                    title = "HD Video MP4",
                    desc = "Perfect with sound tracks, high resolution",
                    icon = Icons.Default.VideoFile,
                    color = Color(0xFF6750A4),
                    onClick = { onExport("MP4 Movie") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export Layer Vectors as JSON
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExport("Vector Layers JSON") },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF6750A4), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Export Layer Vectors (JSON)", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Highly granular layered layout for developer import", color = Color(0xFF49454F), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportFormatCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(title, color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(desc, color = Color(0xFF49454F), fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
    }
}
