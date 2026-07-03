package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.EditorWorkspace
import com.example.ui.screens.ProjectDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AnimationViewModel

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalAnimationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF0F172A) // Sleek slate dark background matches the theme
        ) {
          val viewModel: AnimationViewModel = viewModel()
          val currentProjectId by viewModel.currentProjectId.collectAsState()

          AnimatedContent(
            targetState = currentProjectId,
            transitionSpec = {
              fadeIn(animationSpec = tween(350)) with fadeOut(animationSpec = tween(250))
            },
            label = "ScreenTransition"
          ) { projectId ->
            if (projectId == null) {
              ProjectDashboard(
                viewModel = viewModel,
                onProjectSelected = { id -> viewModel.selectProject(id) }
              )
            } else {
              EditorWorkspace(
                viewModel = viewModel,
                onBackToDashboard = { viewModel.selectProject(null) }
              )
            }
          }
        }
      }
    }
  }
}
