package com.example.quickstage.ui

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.quickstage.ui.viewmodel.AppViewModel
import com.example.quickstage.ui.viewmodel.ScanStatus
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun ScannerScreen(viewModel: AppViewModel, onScanComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()

    // Handle Popup
    when (val status = scanStatus) {
        is ScanStatus.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetScanStatus() },
                title = { Text("Success") },
                text = { Text(status.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetScanStatus() }) {
                        Text("OK")
                    }
                }
            )
        }
        is ScanStatus.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetScanStatus() },
                title = { Text("Error") },
                text = { Text(status.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetScanStatus() }) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        val scanner = BarcodeScanning.getClient()
                        
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { value ->
                                        // Stop logging, just process
                                        viewModel.processScan(value)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                // Handle failure
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("Scanner", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
