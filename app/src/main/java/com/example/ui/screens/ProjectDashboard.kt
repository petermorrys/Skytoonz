package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.viewmodel.AnimationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboard(
    viewModel: AnimationViewModel,
    onProjectSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    
    // Bottom navigation / tab state
    var activeTab by remember { mutableStateOf("Projects") }
    var showLearnModal by remember { mutableStateOf(false) }

    // Screen State: standard list vs full-screen creation view
    var isCreatingProject by remember { mutableStateOf(false) }

    // New Project State variables
    var newProjectName by remember { mutableStateOf("") }
    var newProjectFps by remember { mutableStateOf(12f) }
    var selectedBgType by remember { mutableStateOf("White") }
    
    // Canvas dimensions (width, height, label)
    var selectedCanvasSize by remember { mutableStateOf(CanvasSizePreset.YOUTUBE) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFDF8FD)) // Warm premium background
    ) {
        if (!isCreatingProject) {
            // Main Dashboard Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Custom App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE91E63)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎨",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Art Animator",
                            color = Color(0xFF1E1E2F),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    // Top quick actions / indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { showLearnModal = true }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5E3EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💡", fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Projects vs Clips Toggle Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3EDF4))
                        .padding(4.dp)
                ) {
                    TabToggleButton(
                        text = "Projects",
                        isActive = activeTab == "Projects",
                        onClick = { activeTab = "Projects" },
                        modifier = Modifier.weight(1f)
                    )
                    TabToggleButton(
                        text = "Clips",
                        isActive = activeTab == "Clips",
                        onClick = { activeTab = "Clips" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Body
                if (activeTab == "Projects") {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Demo projects section (If user has preloaded demo)
                        val demoProject = projects.find { it.id == 9999L }
                        if (demoProject != null) {
                            item {
                                Text(
                                    text = "Demo Projects",
                                    color = Color(0xFFE91E63),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                DemoProjectCard(
                                    project = demoProject,
                                    onClick = { onProjectSelected(demoProject.id) }
                                )
                            }
                        }

                        // User projects section
                        val userProjects = projects.filter { it.id != 9999L }
                        if (userProjects.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Your Animation Sequences",
                                    color = Color(0xFF6B7280),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )
                            }
                            items(userProjects) { project ->
                                UserProjectItemCard(
                                    project = project,
                                    onClick = { onProjectSelected(project.id) },
                                    onDelete = {
                                        viewModel.selectProject(project.id)
                                        viewModel.deleteCurrentProject()
                                    }
                                )
                            }
                        } else if (projects.size <= 1 && demoProject != null) {
                            // Only has demo, list empty otherwise
                            item {
                                EmptyStateCard(onCreateClick = { isCreatingProject = true })
                            }
                        } else if (projects.isEmpty()) {
                            item {
                                EmptyStateCard(onCreateClick = { isCreatingProject = true })
                            }
                        }
                    }
                } else {
                    // Clips Tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFCE7F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                icon = AppIconType.VIDEO_LIBRARY,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Clips Exported Yet",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E2F),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Open an animation in the editor, click the Share icon, and export as GIF or video to populate your clips library!",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Custom persistent Bottom Navigation containing Home, FAB (+), and Learn
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.White)
                        )
                    )
                    .padding(bottom = 16.dp, top = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(64.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF3EDF4), RoundedCornerShape(20.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Home Action
                    Column(
                        modifier = Modifier
                            .clickable { activeTab = "Projects" }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (activeTab == "Projects") "🏠" else "🏚️",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Home",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == "Projects") Color(0xFFE91E63) else Color(0xFF6B7280)
                        )
                    }

                    // Floating Pink Plus Action
                    FloatingActionButton(
                        onClick = {
                            newProjectName = ""
                            newProjectFps = 12f
                            selectedBgType = "White"
                            selectedCanvasSize = CanvasSizePreset.YOUTUBE
                            isCreatingProject = true
                        },
                        containerColor = Color(0xFFE91E63),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .offset(y = (-18).dp)
                            .size(56.dp)
                            .shadow(6.dp, CircleShape)
                            .testTag("create_project_btn")
                    ) {
                        AppIcon(
                            icon = AppIconType.ADD,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Learn/Tutorials Action
                    Column(
                        modifier = Modifier
                            .clickable { showLearnModal = true }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📖",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Learn",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showLearnModal) Color(0xFFE91E63) else Color(0xFF6B7280)
                        )
                    }
                }
            }
        } else {
            // FULL SCREEN "NEW PROJECT" CREATION FLOW (Matches requested design image)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row (X button left, Title Center)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isCreatingProject = false }) {
                        AppIcon(
                            icon = AppIconType.CLOSE,
                            tint = Color(0xFF1E1E2F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "New Project",
                        color = Color(0xFF1E1E2F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Section 1: Project Name
                    Text(
                        text = "Project Name",
                        color = Color(0xFF1E1E2F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        placeholder = { Text("e.g. Jumping Squirrel 🐿️") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF49454F),
                            focusedBorderColor = Color(0xFFE91E63),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedLabelColor = Color(0xFFE91E63)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_name_field"),
                        trailingIcon = {
                            if (newProjectName.isNotEmpty()) {
                                IconButton(onClick = { newProjectName = "" }) {
                                    AppIcon(AppIconType.CLOSE, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section 2: Background Choices
                    Text(
                        text = "Background Style",
                        color = Color(0xFF1E1E2F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Bento Grid-like structure of background presets (Grid of swatches)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFAF6FA))
                            .border(1.dp, Color(0xFFF3EDF4), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val row1 = listOf("White", "Black", "Gray", "Cream")
                        val row2 = listOf("Blue", "Green", "Pink", "None")
                        val row3 = listOf("Ruled", "Grid", "Dotted", "Sketch")
                        val row4 = listOf("Kraft", "Chalk")

                        BackgroundRowSwatches(row1, selectedBgType) { selectedBgType = it }
                        BackgroundRowSwatches(row2, selectedBgType) { selectedBgType = it }
                        BackgroundRowSwatches(row3, selectedBgType) { selectedBgType = it }
                        BackgroundRowSwatches(row4, selectedBgType) { selectedBgType = it }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section 3: Canvas Size (Cinematic Presets)
                    Text(
                        text = "Canvas Size",
                        color = Color(0xFF1E1E2F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasSizePreset.values().forEach { preset ->
                            val isSelected = selectedCanvasSize == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFFFF1F6) else Color(0xFFFAFAFB))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFE91E63) else Color(0xFFE5E7EB),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCanvasSize = preset }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = preset.label,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFE91E63) else Color(0xFF1E1E2F),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${preset.width} x ${preset.height} (${preset.aspectRatio})",
                                        color = Color(0xFF6B7280),
                                        fontSize = 12.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFE91E63) else Color.Transparent)
                                        .border(2.dp, if (isSelected) Color(0xFFE91E63) else Color(0xFFD1D5DB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        AppIcon(
                                            icon = AppIconType.CHECK_CIRCLE,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section 4: FPS Playback Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame Rate (FPS)",
                            color = Color(0xFF1E1E2F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${newProjectFps.toInt()} FPS",
                            color = Color(0xFFE91E63),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }

                    Slider(
                        value = newProjectFps,
                        onValueChange = { newProjectFps = it },
                        valueRange = 1f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE91E63),
                            activeTrackColor = Color(0xFFE91E63),
                            inactiveTrackColor = Color(0xFFF3EDF7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "12 FPS is cartoon work standard. 24 FPS for clean movie cinematics.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Big pink main action button
                    Button(
                        onClick = {
                            viewModel.createNewProject(
                                name = newProjectName,
                                fps = newProjectFps.toInt(),
                                width = selectedCanvasSize.width,
                                height = selectedCanvasSize.height,
                                backgroundType = selectedBgType
                            )
                            isCreatingProject = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = "CREATE PROJECT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        // Educational Learn Tips Dialog
        if (showLearnModal) {
            AlertDialog(
                onDismissRequest = { showLearnModal = false },
                containerColor = Color.White,
                titleContentColor = Color(0xFF1E1E2F),
                textContentColor = Color(0xFF4B5563),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Animation Masterclass", fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LearnTipItem(
                            number = "1",
                            title = "The 12 FPS Standard",
                            description = "Most classic Disney cartoon and anime hand-drawn movies are animated 'on twos' at 12 frames per second. It provides a organic feel and saves drawing time!"
                        )
                        LearnTipItem(
                            number = "2",
                            title = "Using Layers",
                            description = "Keep your stationary scenery on the 'Background' layer, main characters on 'Layer 1', and dynamic foreground elements (e.g. rain, particle sparks) on 'Foreground'."
                        )
                        LearnTipItem(
                            number = "3",
                            title = "Squash and Stretch",
                            description = "To make impacts look juicy and organic, deform your objects on action frames. Squash objects flat when they land on a surface, and stretch them elongated while falling!"
                        )
                        LearnTipItem(
                            number = "4",
                            title = "Onion Skinning helper",
                            description = "Onion skinning allows you to see transparent ghost shadows of the previous (red) and next (green) frames. Use it in canvas settings to guide smooth motion transitions!"
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showLearnModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Text("Got it!", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun TabToggleButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color.White else Color.Transparent)
            .shadow(if (isActive) 2.dp else 0.dp, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color(0xFFE91E63) else Color(0xFF6B7280),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun BackgroundRowSwatches(
    types: List<String>,
    selectedType: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        types.forEach { type ->
            val isSelected = selectedType == type
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(type) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getRepresentationalColor(type))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFFE91E63) else Color(0xFFCAC4D0).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    // Draw representational pattern inside box
                    DrawBackgroundSwatchPattern(type)

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE91E63)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = type,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFFE91E63) else Color(0xFF4B5563),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DrawBackgroundSwatchPattern(type: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (type) {
            "None" -> {
                // Mini checkers
                val cellSize = 8f
                for (i in 0 until (w / cellSize).toInt() + 1) {
                    for (j in 0 until (h / cellSize).toInt() + 1) {
                        if ((i + j) % 2 == 0) {
                            drawRect(
                                color = Color(0xFFE0E0E0),
                                topLeft = Offset(i * cellSize, j * cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
            "Ruled" -> {
                // Lines horizontal + vertical red margin
                val step = 10f
                for (i in 1..4) {
                    drawLine(
                        color = Color(0xFF93C5FD).copy(alpha = 0.5f),
                        start = Offset(0f, i * step + 6f),
                        end = Offset(w, i * step + 6f),
                        strokeWidth = 1f
                    )
                }
                drawLine(
                    color = Color(0xFFFCA5A5).copy(alpha = 0.7f),
                    start = Offset(14f, 0f),
                    end = Offset(14f, h),
                    strokeWidth = 1f
                )
            }
            "Grid" -> {
                val step = 10f
                for (i in 0..(w / step).toInt()) {
                    drawLine(color = Color(0xFFE5E7EB), start = Offset(i * step, 0f), end = Offset(i * step, h), strokeWidth = 0.5f)
                }
                for (j in 0..(h / step).toInt()) {
                    drawLine(color = Color(0xFFE5E7EB), start = Offset(0f, j * step), end = Offset(w, j * step), strokeWidth = 0.5f)
                }
            }
            "Dotted" -> {
                val step = 12f
                for (i in 1..(w / step).toInt()) {
                    for (j in 1..(h / step).toInt()) {
                        drawCircle(color = Color(0xFF9CA3AF), radius = 1f, center = Offset(i * step, j * step))
                    }
                }
            }
            "Sketch" -> {
                // Diagonals
                val step = 15f
                for (i in 0..10) {
                    drawLine(
                        color = Color(0xFFE5E7EB),
                        start = Offset(i * step, 0f),
                        end = Offset(0f, i * step),
                        strokeWidth = 0.8f
                    )
                }
            }
            "Kraft" -> {
                // Diagonals brown
                val step = 20f
                for (i in 0..6) {
                    drawLine(
                        color = Color(0xFFC4A484).copy(alpha = 0.3f),
                        start = Offset(i * step, 0f),
                        end = Offset(0f, i * step),
                        strokeWidth = 1f
                    )
                }
            }
            "Chalk" -> {
                val step = 16f
                for (i in 0..(w / step).toInt()) {
                    drawLine(color = Color(0xFF22543D).copy(alpha = 0.3f), start = Offset(i * step, 0f), end = Offset(i * step, h), strokeWidth = 0.8f)
                }
            }
        }
    }
}

fun getRepresentationalColor(type: String): Color {
    return when (type) {
        "White" -> Color.White
        "Black" -> Color(0xFF1E1E1E)
        "Gray" -> Color(0xFFE5E7EB)
        "Cream" -> Color(0xFFFDFBF7)
        "Blue" -> Color(0xFFE0F2FE)
        "Green" -> Color(0xFFDCFCE7)
        "Pink" -> Color(0xFFFCE7F3)
        "None" -> Color.White
        "Ruled" -> Color(0xFFFDFBF7)
        "Grid" -> Color(0xFFFBFBFC)
        "Dotted" -> Color(0xFFFAFAFA)
        "Sketch" -> Color(0xFFF3F4F6)
        "Kraft" -> Color(0xFFD2B48C)
        "Chalk" -> Color(0xFF142F24)
        else -> Color.White
    }
}

@Composable
fun DemoProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live vector thumbnail simulation
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF1F6)),
                contentAlignment = Alignment.Center
            ) {
                // Stylized visual representation of jumping cat pink logo
                Text("🐈", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        color = Color(0xFF1E1E2F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFE1EC))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PRO", color = Color(0xFFE91E63), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${project.fps} fps • 3 Frames • Canvas ${project.width}x${project.height}",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE91E63)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = AppIconType.PLAY_ARROW,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun UserProjectItemCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = formatter.format(Date(project.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF3EDF4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5EDF6)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎬", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    color = Color(0xFF1E1E2F),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${project.fps} fps  •  $dateString  •  ${project.width}p",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Background: ${project.backgroundType}",
                    color = Color(0xFFE91E63),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onDelete) {
                AppIcon(
                    icon = AppIconType.OUTLINED_DELETE,
                    tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(1.dp, Color(0xFFF3EDF4), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎬", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No animation projects yet",
                color = Color(0xFF1E1E2F),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the '+' floating pink button below or click the button below to start crafting your first frame-by-frame masterwork!",
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Start Animating", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LearnTipItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAF9FB))
            .border(1.dp, Color(0xFFF3EDF4), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFE91E63)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2F), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color(0xFF4B5563), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

enum class CanvasSizePreset(val label: String, val width: Int, val height: Int, val aspectRatio: String) {
    YOUTUBE("YouTube (16:9 Landscape)", 1920, 1080, "16:9"),
    INSTAGRAM("Instagram (1:1 Square)", 1080, 1080, "1:1"),
    TIKTOK("TikTok / Shorts (9:16 Vertical)", 1080, 1920, "9:16"),
    STANDARD("Standard (4:3 Cartoon)", 1440, 1080, "4:3")
}
