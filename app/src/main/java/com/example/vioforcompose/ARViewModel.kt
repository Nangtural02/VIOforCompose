package com.example.vioforcompose

import android.content.Context
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.filament.Engine
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.getDescription
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class ARViewModel: ViewModel() {
    private val _VIOtext = MutableStateFlow("empty")
    val VIOtext: StateFlow<String> = _VIOtext

    private val _trackingState = MutableStateFlow<String>("")
    val trackingState: StateFlow<String> = _trackingState
    val isChildNodeEmpty = mutableStateOf(true)

    val targetPosition = MutableStateFlow<Point?>(null)

    fun updateVIOText(text: String) {
        _VIOtext.value = text
    }

    fun updateTrackingFailureReason(context : Context, reason: TrackingFailureReason?) {
         reason?.let {
            _trackingState.value = it.getDescription(context)
        } ?: if (isChildNodeEmpty.value) {
             _trackingState.value = "Move phone more"
        } else {
             _trackingState.value = "VIO Position Good"
        }
    }

}
data class Point(var x: Float = 0f, var y: Float = 0f, var z: Float =0f)
