package com.islam.camerax

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    lateinit var cameraExecutor: Executor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            val imageCapture = remember {
                ImageCapture.Builder().build()
            }


            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(modifier = Modifier.fillMaxSize(), imageCapture = imageCapture)
                IconButton(modifier = Modifier.align(alignment = Alignment.BottomCenter),
                    onClick = { captureImage(imageCapture) }) {
                    Icon(
                        modifier = Modifier.size(192.dp),
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = "icon_camera"
                    )
                }
            }

        }
    }

    private fun captureImage(imageCapture: ImageCapture) {

        val file = File.createTempFile("img", ".jpg")
        val outPutFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(outPutFileOptions,cameraExecutor,object :ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                println("the Uri is ${outputFileResults.savedUri}")
            }

            override fun onError(exception: ImageCaptureException) {

            }

        })
    }
}


@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    imageCapture: ImageCapture
) {

    val lifeCycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    AndroidView(factory = { context ->
        val previewView = PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            this.scaleType = scaleType
        }

        val previewUseCase = Preview.Builder().build()
        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)

        coroutineScope.launch {
            val cameraProvider = context.cameraProvider()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifeCycleOwner, cameraSelector, previewUseCase, imageCapture
            )

        }

        previewView
    })

}

suspend fun Context.cameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val listenableFeatures = ProcessCameraProvider.getInstance(this)
    listenableFeatures.addListener({
        continuation.resume(listenableFeatures.get())
    }, ContextCompat.getMainExecutor(this))
}

