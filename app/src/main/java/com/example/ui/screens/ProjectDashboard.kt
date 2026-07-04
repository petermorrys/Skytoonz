package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.viewmodel.AnimationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectDashboard(
    viewModel: AnimationViewModel,
    onProjectSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    var showCreateDialog by remember { mutableStateFlowOf(false) }
    var newProjectName by remember { mutableStateFlowOf("") }
    var newProjectFps by remember { mutableStateFlowOf(12f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2FA)) // Bento Pale lavender/grey Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Bento Elegant Header Visual Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEADDFF), // Pale purple
                                Color(0xFFE8DEF8), // Lavender
                                Color(0xFFFFD8E4)  // Pale pink
                            )
                        )
                    )
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "ANIMATION STUDIO",
                        color = Color(0xFF21005D), // Deep purple
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Frame by Frame Craft",
                        color = Color(0xFF21005D), // Deep purple
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Stylus optimized, zero latency vector canvas",
                        color = Color(0xFF49454F), // Slate text
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Projects",
                    color = Color(0xFF1D1B20), // Bento Dark Text
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)), // Bento Primary Purple
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_project_btn")
                ) {
                    AppIcon(AppIconType.ADD, contentDescription = "New Project")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Project", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (projects.isEmpty()) {
                // Friendly Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        AppIcon(
                            icon = AppIconType.FOLDER_OPEN,
                            tint = Color(0xFFCAC4D0), // Soft grey
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No projects yet",
                            color = Color(0xFF1D1B20),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create your first professional animation sequence! Click the button above to begin.",
                            color = Color(0xFF49454F),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(projects) { project ->
                        ProjectItemRow(
                            project = project,
                            onClick = { onProjectSelected(project.id) },
                            onDelete = { viewModel.selectProject(project.id); viewModel.deleteCurrentProject() }
                        )
                    }
                }
            }
        }

        // Create Project Dialog (Bento Styled)
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = Color.White,
                titleContentColor = Color(0xFF1D1B20),
                textContentColor = Color(0xFF49454F),
                title = { Text("New Animation Canvas", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = { newProjectName = it },
                            label = { Text("Project Title") },
                            placeholder = { Text("e.g. Bouncing Ball") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF49454F),
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_name_field")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Playback Speed:", color = Color(0xFF1D1B20), fontWeight = FontWeight.Medium)
                            Text("${newProjectFps.toInt()} FPS", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = newProjectFps,
                            onValueChange = { newProjectFps = it },
                            valueRange = 1f..30f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6750A4),
                                activeTrackColor = Color(0xFF6750A4),
                                inactiveTrackColor = Color(0xFFF3EDF7)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "12 FPS is ideal for standard cartoon work. 24 FPS for cinematics.",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createNewProject(newProjectName, newProjectFps.toInt())
                            showCreateDialog = false
                            newProjectName = ""
                            newProjectFps = 12f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = Color(0xFF6750A4))
                    }
                }
            )
        }
    }
}

@Composable
fun ProjectItemRow(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()) }
    val dateString = formatter.format(Date(project.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3EDF7)), // Pale lavender background
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = AppIconType.MOVIE,
                    tint = Color(0xFF6750A4), // M3 Primary Purple
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    color = Color(0xFF1D1B20), // Bento Dark Text
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${project.fps} FPS  •  $dateString",
                        color = Color(0xFF49454F), // Bento Slate Grey
                        fontSize = 12.sp
                    )
                }
                if (project.audioTrackName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppIcon(AppIconType.MUSIC_NOTE, contentDescription = "Audio track", tint = Color(0xFF6750A4), modifier = Modifier.size(12.dp))
                        Text(project.audioTrackName, color = Color(0xFF6750A4), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Sync Status
            AppIcon(
                icon = if (project.isSynced) AppIconType.CLOUD_DONE else AppIconType.CLOUD_QUEUE,
                contentDescription = if (project.isSynced) "Synced" else "Local only",
                tint = if (project.isSynced) Color(0xFF22C55E) else Color(0xFFCAC4D0),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(onClick = onDelete) {
                AppIcon(
                    icon = AppIconType.OUTLINED_DELETE,
                    contentDescription = "Delete project",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

// Simple Helper for StateFlow instantiation
fun <T> mutableStateFlowOf(value: T): MutableState<T> = mutableStateOf(value)
