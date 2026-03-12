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
    // ✅ Use the SAME EglBase from WebRTCManager — this fixes the black box
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // ✅ Use shared eglBase, not a new one
                init(webRTCManager.eglBase.eglBaseContext, null)
                setMirror(true)          // Mirror for selfie cam
                setEnableHardwareScaler(true)
                webRTCManager.localVideoTrack.addSink(this)
            }
        },
        update = { view ->
            // Re-add sink if track changes
            webRTCManager.localVideoTrack.addSink(view)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // Cleanup handled by WebRTCManager.release()
        }
    }
}