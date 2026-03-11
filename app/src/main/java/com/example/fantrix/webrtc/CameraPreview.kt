package com.example.fantrix.webrtc

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

@Composable
fun CameraPreview(
    webRTCManager: WebRTCManager,
    modifier: Modifier = Modifier
) {

    val eglBase = remember { EglBase.create() }

    AndroidView(
        modifier = modifier,
        factory = { context ->

            SurfaceViewRenderer(context).apply {

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                init(eglBase.eglBaseContext, null)
                setMirror(true)
                setEnableHardwareScaler(true)

                webRTCManager.localVideoTrack.addSink(this)
            }
        }
    )
}