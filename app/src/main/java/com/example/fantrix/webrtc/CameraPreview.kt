package com.example.fantrix.webrtc

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer

@Composable
fun CameraPreview(
    webRTCManager: WebRTCManager,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                init(webRTCManager.eglBase.eglBaseContext, null)
                setMirror(true)
                setEnableHardwareScaler(true)
                webRTCManager.getLocalVideoTrack()?.addSink(this)
            }
        },
        update = { view ->
            // view is the SurfaceViewRenderer — use it directly
            webRTCManager.getLocalVideoTrack()?.removeSink(view)
            webRTCManager.getLocalVideoTrack()?.addSink(view)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // Cleanup handled by WebRTCManager.release()
        }
    }
}