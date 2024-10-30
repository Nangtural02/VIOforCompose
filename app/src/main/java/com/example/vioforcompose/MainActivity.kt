package com.example.vioforcompose

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vioforcompose.ui.theme.SerialViewModel
import com.example.vioforcompose.ui.theme.VIOforComposeTheme
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val arVIewModel: ARViewModel by viewModels()
    private val serialViewmodel by viewModels<SerialViewModel>{
        SerialViewModelFactory(application, arVIewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            VIOforComposeTheme {
                Column() {
                    val VIOtext:String = arVIewModel.VIOtext.collectAsState().value
                    val SerialText:String = serialViewmodel.blockString.collectAsState().value
                    val coroutineScope = rememberCoroutineScope()
                    val context = LocalContext.current
                    Box(modifier = Modifier){
                        Text(text = VIOtext)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically

                    ){

                        Text(text = SerialText)
                        TextButton(onClick =
                        {
                            serialViewmodel.connectSerialDevice(context = context)
                        } , content = {
                            Text("Connect")
                        }
                        )
                    }
                    TextButton(onClick =
                    {
                        arVIewModel.targetPosition.value = Point(3f,1f,3f)
                    } , content = {
                        Text("make Something")
                    }
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    ARScreen(arViewModel = arVIewModel, modifier = Modifier)
                }
            }
        }
    }
    override fun onResume(){
        super.onResume()
        arVIewModel.isChildNodeEmpty.value = true

    }


}