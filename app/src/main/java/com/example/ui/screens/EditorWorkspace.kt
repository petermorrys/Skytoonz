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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val timelineListState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto scroll timeline to the active frame item during playback or selection!
    LaunchedEffect(currentFrameIndex) {
        if (frames.isNotEmpty()) {
            try {
                timelineListState.animateScrollToItem(currentFrameIndex)
            } catch (e: Exception) {
                // Safely catch any list scroll state errors
            }
        }
    }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(1500)
            feedbackMessage = null
        }
    }

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
                        AppIcon(AppIconType.ARROW_BACK, contentDescription = "Dashboard", tint = Color(0xFF1D1B20))
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
                        AppIcon(AppIconType.UNDO, contentDescription = "Undo", tint = Color(0xFF1D1B20))
                    }
                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        AppIcon(AppIconType.REDO, contentDescription = "Redo", tint = Color(0xFF1D1B20))
                    }
                    // Sync Status
                    IconButton(onClick = { viewModel.toggleCloudSync() }) {
                        val iconType = if (project?.isSynced == true) AppIconType.CLOUD_DONE else AppIconType.CLOUD_QUEUE
                        val tint = if (project?.isSynced == true) Color(0xFF22C55E) else Color(0xFF1D1B20)
                        AppIcon(iconType, contentDescription = "Cloud Sync", tint = tint)
                    }
                    // Settings Drawer Trigger
                    IconButton(onClick = { activePanel = if (activePanel == ActivePanel.SETTINGS) null else ActivePanel.SETTINGS }) {
                        AppIcon(AppIconType.SETTINGS, contentDescription = "Canvas Settings", tint = Color(0xFF1D1B20))
                    }
                    // Share/Export Trigger
                    IconButton(onClick = { activePanel = if (activePanel == ActivePanel.EXPORT) null else ActivePanel.EXPORT }) {
                        AppIcon(AppIconType.SHARE, contentDescription = "Export Animation", tint = Color(0xFF1D1B20))
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

                // 1. Floating Horizontal Tools Bar (top-center)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Drag Handle symbol
                    AppIcon(
                        icon = AppIconType.DRAG_INDICATOR,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.LightGray))

                    // Brush Button
                    val isBrushSelected = brushConfig.type != BrushType.ERASER
                    IconButton(
                        onClick = {
                            viewModel.updateBrushType(BrushType.PEN)
                            feedbackMessage = "Brush tool active"
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBrushSelected) Color(0xFFFFE1EC) else Color.Transparent)
                    ) {
                        AppIcon(
                            icon = AppIconType.BRUSH,
                            contentDescription = "Brush Tool",
                            tint = if (isBrushSelected) Color(0xFFE91E63) else Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Eraser Button
                    val isEraserSelected = brushConfig.type == BrushType.ERASER
                    IconButton(
                        onClick = {
                            viewModel.updateBrushType(BrushType.ERASER)
                            feedbackMessage = "Eraser tool active"
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEraserSelected) Color(0xFFFFE1EC) else Color.Transparent)
                    ) {
                        AppIcon(
                            icon = AppIconType.AUTO_FIX_NORMAL,
                            contentDescription = "Eraser Tool",
                            tint = if (isEraserSelected) Color(0xFFE91E63) else Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Lasso Button
                    var isLassoSelected by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            isLassoSelected = !isLassoSelected
                            feedbackMessage = if (isLassoSelected) "Lasso selection tool activated" else "Lasso deactivated"
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLassoSelected) Color(0xFFFFE1EC) else Color.Transparent)
                    ) {
                        AppIcon(
                            icon = AppIconType.GESTURE,
                            contentDescription = "Lasso Selection Tool",
                            tint = if (isLassoSelected) Color(0xFFE91E63) else Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Paint Bucket Button
                    var isBucketSelected by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            isBucketSelected = !isBucketSelected
                            feedbackMessage = if (isBucketSelected) "Paint Bucket tool activated" else "Bucket deactivated"
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBucketSelected) Color(0xFFFFE1EC) else Color.Transparent)
                    ) {
                        AppIcon(
                            icon = AppIconType.FORMAT_COLOR_FILL,
                            contentDescription = "Paint Bucket Tool",
                            tint = if (isBucketSelected) Color(0xFFE91E63) else Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Text Tool Button
                    var isTextSelected by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            isTextSelected = !isTextSelected
                            feedbackMessage = if (isTextSelected) "Text overlay tool activated" else "Text deactivated"
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTextSelected) Color(0xFFFFE1EC) else Color.Transparent)
                    ) {
                        AppIcon(
                            icon = AppIconType.TITLE,
                            contentDescription = "Text Tool",
                            tint = if (isTextSelected) Color(0xFFE91E63) else Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 2. Floating Vertical Tools Bar (middle-right)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag Handle
                    AppIcon(
                        icon = AppIconType.DRAG_INDICATOR,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )

                    // Brush Settings/Size display/trigger
                    IconButton(
                        onClick = { activePanel = if (activePanel == ActivePanel.BRUSH) null else ActivePanel.BRUSH },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (activePanel == ActivePanel.BRUSH) Color(0xFFEADDFF) else Color.Transparent)
                    ) {
                        Text(
                            text = "${brushConfig.size.toInt()}px",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F)
                        )
                    }

                    // Active Color Swatch Display
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(brushConfig.color)
                            .border(1.5.dp, Color(0xFFCAC4D0), CircleShape)
                            .clickable { activePanel = ActivePanel.BRUSH }
                    )

                    // Ruler Tool Button
                    val isRulerActive by viewModel.rulerEnabled.collectAsState()
                    IconButton(
                        onClick = {
                            viewModel.setRulerEnabled(!isRulerActive)
                            feedbackMessage = if (!isRulerActive) "Ruler guides active" else "Ruler deactivated"
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRulerActive) Color(0xFFE91E63) else Color.Transparent)
                    ) {
                        AppIcon(
                            icon = AppIconType.RULER,
                            contentDescription = "Ruler Guides",
                            tint = if (isRulerActive) Color.White else Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                        AppIcon(AppIconType.VOLUME_UP, contentDescription = "Audio track playing", tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${project?.audioTrackName} (${(audioProgress * 100).toInt()}%)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // In-app Premium Toast Alert Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = feedbackMessage != null,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2E2E38).copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = feedbackMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- ACTIVE RULER BAR (Pops up above the timeline when ruler is enabled) ---
            val rulerEnabled by viewModel.rulerEnabled.collectAsState()
            val selectedRuler by viewModel.selectedRuler.collectAsState()
            val rulerLocked by viewModel.rulerLocked.collectAsState()

            androidx.compose.animation.AnimatedVisibility(
                visible = rulerEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(Color(0xFF2E2E38), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock/Unlock Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (rulerLocked) Color(0xFFE91E63) else Color.White.copy(alpha = 0.15f))
                            .clickable { viewModel.setRulerLocked(!rulerLocked) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            icon = if (rulerLocked) AppIconType.LOCK else AppIconType.LOCK_OPEN,
                            contentDescription = "Lock Ruler",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (rulerLocked) "LOCKED" else "UNLOCK",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Ruler type selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("LINE", "LINE", AppIconType.LINE),
                            Triple("CIRC", "CIRC", AppIconType.CIRCLE),
                            Triple("BOX", "BOX", AppIconType.BOX),
                            Triple("MIRR", "MIRR", AppIconType.MIRROR)
                        ).forEach { (typeKey, labelText, iconType) ->
                            val isSel = selectedRuler == typeKey
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFFFE1EC) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) Color(0xFFE91E63) else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setSelectedRuler(typeKey) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AppIcon(
                                    icon = iconType,
                                    contentDescription = labelText,
                                    tint = if (isSel) Color(0xFFE91E63) else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = labelText,
                                    color = if (isSel) Color(0xFFE91E63) else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Close ruler button
                    IconButton(
                        onClick = { viewModel.setRulerEnabled(false) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        AppIcon(AppIconType.CLOSE, contentDescription = "Close Ruler", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- BOTTOM TIMELINE CONTAINER (Floating Bento Card above bottom bar) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // --- Frame Quick Actions Bar ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F7FA))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame Actions (F${currentFrameIndex + 1}/${frames.size})",
                            color = Color(0xFF6750A4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Move Left
                            val canMoveLeft = currentFrameIndex > 0
                            Row(
                                modifier = Modifier
                                    .clickable(enabled = canMoveLeft) {
                                        viewModel.moveFrame(currentFrameIndex, currentFrameIndex - 1)
                                        feedbackMessage = "Frame moved left!"
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    icon = AppIconType.ARROW_LEFT,
                                    contentDescription = "Move Left",
                                    tint = if (canMoveLeft) Color(0xFFE91E63) else Color(0xFFCAC4D0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Left",
                                    color = if (canMoveLeft) Color(0xFF1D1B20) else Color(0xFFCAC4D0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Move Right
                            val canMoveRight = currentFrameIndex < frames.size - 1
                            Row(
                                modifier = Modifier
                                    .clickable(enabled = canMoveRight) {
                                        viewModel.moveFrame(currentFrameIndex, currentFrameIndex + 1)
                                        feedbackMessage = "Frame moved right!"
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Right",
                                    color = if (canMoveRight) Color(0xFF1D1B20) else Color(0xFFCAC4D0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                AppIcon(
                                    icon = AppIconType.ARROW_RIGHT,
                                    contentDescription = "Move Right",
                                    tint = if (canMoveRight) Color(0xFFE91E63) else Color(0xFFCAC4D0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Vertical Divider
                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0xFFCAC4D0).copy(alpha = 0.5f)))

                            // Duplicate
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        viewModel.duplicateCurrentFrame()
                                        feedbackMessage = "Frame duplicated!"
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("duplicate_frame_btn"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    icon = AppIconType.CONTENT_COPY,
                                    contentDescription = "Duplicate Frame",
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Duplicate",
                                    color = Color(0xFF1D1B20),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Vertical Divider
                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0xFFCAC4D0).copy(alpha = 0.5f)))

                            // Delete
                            val canDelete = frames.size > 1
                            Row(
                                modifier = Modifier
                                    .clickable(enabled = canDelete) {
                                        viewModel.deleteCurrentFrame()
                                        feedbackMessage = "Frame deleted!"
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("delete_frame_btn"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    icon = AppIconType.DELETE,
                                    contentDescription = "Delete Frame",
                                    tint = if (canDelete) Color(0xFFBA1A1A) else Color(0xFFCAC4D0),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Delete",
                                    color = if (canDelete) Color(0xFFBA1A1A) else Color(0xFFCAC4D0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Controls (Left)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(onClick = { viewModel.prevFrame() }, modifier = Modifier.size(32.dp)) {
                                AppIcon(AppIconType.SKIP_PREVIOUS, contentDescription = "Prev Frame", tint = Color(0xFF1D1B20), modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { viewModel.togglePlayback() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE91E63))
                                    .testTag("play_pause_fab")
                            ) {
                                AppIcon(
                                    icon = if (isPlaying) AppIconType.PAUSE else AppIconType.PLAY_ARROW,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.nextFrame() }, modifier = Modifier.size(32.dp)) {
                                AppIcon(AppIconType.SKIP_NEXT, contentDescription = "Next Frame", tint = Color(0xFF1D1B20), modifier = Modifier.size(20.dp))
                            }
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFCAC4D0).copy(alpha = 0.6f)))

                        // Scrollable Timeline of Frames
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            LazyRow(
                                state = timelineListState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemsIndexed(frames) { index, frame ->
                                    val isSelected = index == currentFrameIndex
                                    Box(
                                        modifier = Modifier
                                            .size(width = 54.dp, height = 44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color.White else Color(0xFFF3EDF7))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFE91E63) else Color(0xFFCAC4D0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.selectFrameIndex(index) }
                                    ) {
                                        // Custom visual representation for frames with a tiny cute badge
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "F${index + 1}",
                                                color = if (isSelected) Color(0xFFE91E63) else Color(0xFF49454F),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )

                                            // Cute red badge with frame number
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Red)
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        color = Color.White,
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // PLUS BUTTON for adding frame in dashed boundary
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE91E63).copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.addNewFrame() }
                                .testTag("add_frame_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(AppIconType.ADD, contentDescription = "Add Frame", tint = Color(0xFFE91E63), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- THE VERY BOTTOM ACTION CONTROLS BAR (Bento Styled Row) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio track sync
                IconButton(onClick = { activePanel = if (activePanel == ActivePanel.AUDIO) null else ActivePanel.AUDIO }) {
                    val hasAudio = project?.audioTrackName != null
                    AppIcon(
                        icon = AppIconType.MUSIC_NOTE,
                        contentDescription = "Audio track sync",
                        tint = if (hasAudio) Color(0xFF22C55E) else Color(0xFF49454F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Undo
                IconButton(onClick = { viewModel.undo() }) {
                    AppIcon(AppIconType.UNDO, contentDescription = "Undo stroke", tint = Color(0xFF1D1B20), modifier = Modifier.size(24.dp))
                }

                // Redo
                IconButton(onClick = { viewModel.redo() }) {
                    AppIcon(AppIconType.REDO, contentDescription = "Redo stroke", tint = Color(0xFF1D1B20), modifier = Modifier.size(24.dp))
                }

                // Copy Paths
                IconButton(onClick = {
                    viewModel.copyCurrentFrameLayerPaths()
                    feedbackMessage = "Layer strokes copied!"
                }) {
                    AppIcon(AppIconType.CONTENT_COPY, contentDescription = "Copy Layer", tint = Color(0xFF1D1B20), modifier = Modifier.size(24.dp))
                }

                // Paste Paths
                IconButton(onClick = {
                    viewModel.pasteFrameLayerPaths()
                    feedbackMessage = "Layer strokes pasted!"
                }) {
                    AppIcon(AppIconType.CONTENT_PASTE, contentDescription = "Paste Layer", tint = Color(0xFF1D1B20), modifier = Modifier.size(24.dp))
                }

                // Layers Stack Info and selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activePanel == ActivePanel.LAYERS) Color(0xFFEADDFF) else Color.Transparent)
                        .clickable { activePanel = if (activePanel == ActivePanel.LAYERS) null else ActivePanel.LAYERS }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        icon = AppIconType.LAYERS,
                        contentDescription = "Layers list",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${layers.size}",
                        color = Color(0xFF1D1B20),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                        AppIcon(AppIconType.CLOUD_UPLOAD, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cloud Synchronization", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (syncState is SyncProgress.Syncing) {
                            LinearProgressIndicator(color = Color(0xFFEC4899), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Securing project vectors...", color = Color.LightGray, fontSize = 13.sp)
                        } else {
                            AppIcon(AppIconType.CHECK_CIRCLE, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(32.dp))
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
                        AppIcon(AppIconType.VIDEO_LIBRARY, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(48.dp))
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
                                AppIcon(AppIconType.TASK_ALT, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(32.dp))
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
                    AppIcon(AppIconType.CLOSE, contentDescription = "Close", tint = Color(0xFF1D1B20))
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
    var newLayerName by remember { mutableStateOf("") }

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
                    AppIcon(AppIconType.CLOSE, contentDescription = "Close", tint = Color(0xFF1D1B20))
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
                                    AppIcon(
                                        icon = AppIconType.LAYERS,
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
                                    AppIcon(
                                        icon = if (layer.isVisible) AppIconType.VISIBILITY else AppIconType.VISIBILITY_OFF,
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
                    AppIcon(AppIconType.CLOSE, contentDescription = "Close", tint = Color(0xFF1D1B20))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Theme selector
            Text("App Theme Mode", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val themePref by viewModel.themePreference.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("System", "Light", "Dark").forEach { option ->
                    val isSelected = themePref == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFE1EC) else Color(0xFFFAFAFB))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFE91E63) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setThemePreference(option) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) Color(0xFFE91E63) else Color(0xFF4B5563),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))

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
                    AppIcon(AppIconType.CLOSE, contentDescription = "Close", tint = Color(0xFF1D1B20))
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
                        AppIcon(
                            icon = if (isRecording) AppIconType.MIC_OFF else AppIconType.MIC,
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
                            AppIcon(
                                icon = if (isSelected) AppIconType.VOLUME_UP else AppIconType.MUSIC_NOTE,
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
                    AppIcon(AppIconType.CLOSE, contentDescription = "Close", tint = Color(0xFF1D1B20))
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
                    icon = AppIconType.GIF,
                    color = Color(0xFF6750A4),
                    onClick = { onExport("GIF") },
                    modifier = Modifier.weight(1f)
                )

                // Export as Video MP4
                ExportFormatCard(
                    title = "HD Video MP4",
                    desc = "Perfect with sound tracks, high resolution",
                    icon = AppIconType.VIDEO_FILE,
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
                    AppIcon(AppIconType.CODE, tint = Color(0xFF6750A4), modifier = Modifier.size(28.dp))
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
    icon: AppIconType,
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
                AppIcon(icon, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(title, color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(desc, color = Color(0xFF49454F), fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
    }
}
