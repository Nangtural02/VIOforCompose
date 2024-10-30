package com.example.vioforcompose

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberView
import kotlin.math.atan2
import kotlin.math.sqrt


private const val kModelFile = "models/damaged_helmet.glb"
@Composable
fun ARScreen(arViewModel: ARViewModel,modifier:Modifier = Modifier,visibility: Boolean = true) {
    Box(
        modifier = modifier.alpha(if(visibility) 1f else 0f)
    ) {
        // The destroy calls are automatically made when their disposable effect leaves
        // the composition or its key changes.
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val materialLoader = rememberMaterialLoader(engine)
        val cameraNode = rememberARCameraNode(engine)
        val childNodes = rememberNodes()
        val view = rememberView(engine)
        val collisionSystem = rememberCollisionSystem(view)
        var arrowNode by remember { mutableStateOf<Node?>(null) }
        var markerNode = remember { mutableStateOf<Node?>(null) }


        var frame by remember { mutableStateOf<Frame?>(null) }
        val context = LocalContext.current
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            sessionConfiguration = { session, config ->
                config.depthMode =
                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        true -> Config.DepthMode.AUTOMATIC
                        else -> Config.DepthMode.DISABLED
                    }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode =
                    Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            cameraNode = cameraNode,
            planeRenderer = false,
            onTrackingFailureChanged = {
                arViewModel.updateTrackingFailureReason(context, it)
            },
            onSessionUpdated = { _session, updatedFrame ->
                frame = updatedFrame
                //todo: VIO!! 여기!!
                arViewModel.updateVIOText(updatedFrame.camera.pose.toString())
                //

                if (childNodes.isEmpty()) {
                    arViewModel.isChildNodeEmpty.value = false
                    updatedFrame.getUpdatedPlanes()
                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                        ?.let { it.createAnchorOrNull(it.centerPose) }?.let { anchor ->
                            childNodes += createAnchorNode(
                                engine = engine,
                                modelLoader = modelLoader,
                                materialLoader = materialLoader,
                                anchor = anchor
                            )
                        }
                }

                arViewModel.targetPosition.value?.let{ target ->
                    // arrowNode가 없으면 생성하여 childNodes에 추가
                    if (arrowNode == null) {
                        arrowNode = createArrowNode(engine,modelLoader)
                        arrowNode?.let {
                            childNodes.add(it) // 화살표 추가
                            Log.d("asfd","Arrow node created and added to child nodes")
                        }
                    }

                    // arrowNode가 존재할 경우 위치 및 회전 업데이트
                    arrowNode?.let {
                        // 카메라 앞에 화살표 위치 고정
                        positionArrowInFrontOfCamera(frame!!.camera.pose, it)
                        // 화살표가 목표 위치를 가리키도록 회전
                        updateArrowRotationToTarget(it,target)
                        Log.d("asdf","Arrow node position and rotation updated")
                    }
                    // markerNode가 없으면 생성하여 childNodes에 추가
                    if (markerNode.value == null) {
                        markerNode.value = createMarkerNode(engine, modelLoader)
                        markerNode.value?.let { childNodes.add(it) }
                    }

                    // markerNode 위치 업데이트
                    markerNode.value?.let { updateMarkerPosition(target, it) }
                }
            }

        )
        Text(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            color = Color.White,
            text = arViewModel.trackingState.collectAsState().value
        )
    }
}

fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    anchor: Anchor
): AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor)
    val modelNode = ModelNode(
        modelInstance = modelLoader.createModelInstance(kModelFile),
        // Scale to fit in a 0.5 meters cube
        scaleToUnits = 0.5f
    ).apply {
        // Model Node needs to be editable for independent rotation from the anchor rotation
        isEditable = true
        editableScaleRange = 0.2f..0.75f
    }
    val boundingBoxNode = CubeNode(
        engine,
        size = modelNode.extents,
        center = modelNode.center,
        materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
    ).apply {
        isVisible = false
    }
    modelNode.addChildNode(boundingBoxNode)
    anchorNode.addChildNode(modelNode)

    listOf(modelNode, anchorNode).forEach {
        it.onEditingChanged = { editingTransforms ->
            boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
        }
    }
    return anchorNode
}


fun createArrowNode(engine: Engine, modelLoader: ModelLoader): Node {
    val arrowNode = Node(engine)

    // ModelNode 생성하여 화살표 모델 로드
    val arrowModelNode = ModelNode(modelInstance = modelLoader.createModelInstance("models/arrow.glb"))
    arrowModelNode.scale = Scale(0.05f) // 필요한 크기로 조정
    // 기본 축에 대한 회전 보정 (예: X축으로 90도 회전하여 Z축을 향하도록 설정)
    arrowModelNode.rotation = Rotation(-90f, 0f, 90f)
    arrowNode.addChildNode(arrowModelNode) // arrowNode에 ModelNode 추가

    return arrowNode
}
// 화살표를 카메라 앞에 고정하는 함수
fun positionArrowInFrontOfCamera(cameraPose: Pose, arrowNode: Node) {
    val distanceFromCamera = 0.3f // 카메라에서 화살표까지의 거리

    // 카메라 좌표계에서 앞쪽 방향으로 일정 거리만큼 이동한 위치 계산
    val forwardInCameraCoords = floatArrayOf(0f, 0f, -distanceFromCamera)
    val worldPosition = cameraPose.transformPoint(forwardInCameraCoords)

    // 화살표 위치 설정
    arrowNode.position = Position(worldPosition[0], worldPosition[1], worldPosition[2])
}

// 화살표가 목표를 가리키도록 회전시키는 함수
fun updateArrowRotationToTarget(arrowNode: Node, target: Point) {
    val arrowPosition = arrowNode.position
    val dx = target.x - arrowPosition.x
    val dy = target.y - arrowPosition.y
    val dz = target.z - arrowPosition.z

    // 방향 벡터의 길이 계산
    val distanceXZ = sqrt(dx * dx + dz * dz)

    // 각도 계산 (라디안을 도 단위로 변환)
    val yaw = Math.toDegrees(atan2(-dx, -dz).toDouble()).toFloat() // Z축이 앞쪽인 경우
    val pitch = Math.toDegrees(atan2(dy.toDouble(), distanceXZ.toDouble())).toFloat()

    // 화살표 회전 설정
    arrowNode.rotation = Rotation(pitch, yaw, 0f)
}


fun createMarkerNode(engine: Engine, modelLoader: ModelLoader): Node {
    val markerNode = Node(engine)

    // 마커 모델 추가
    val markerModelNode = ModelNode(modelInstance = modelLoader.createModelInstance("models/map_pointer.glb")) //"Map Pointer" (https://skfb.ly/o78E6) by thekiross is licensed under Creative Commons Attribution (http://creativecommons.org/licenses/by/4.0/).
    markerModelNode.scale = Scale(0.3f) // 마커의 크기 설정
    markerNode.addChildNode(markerModelNode) // markerNode에 ModelNode 추가

    return markerNode
}

fun updateMarkerPosition(target: Point, markerNode: Node) {
    markerNode.position = Position(target.x, target.y, target.z) // 목표 위치에서 2m 위로 설정
}

