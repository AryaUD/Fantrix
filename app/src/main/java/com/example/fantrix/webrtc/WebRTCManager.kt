package com.example.fantrix.webrtc

import android.content.Context
import org.webrtc.*

class WebRTCManager(
    private val context: Context
) {

    lateinit var peerConnectionFactory: PeerConnectionFactory
    lateinit var videoCapturer: VideoCapturer
    lateinit var videoSource: VideoSource
    lateinit var localVideoTrack: VideoTrack
    lateinit var audioTrack: AudioTrack
    lateinit var surfaceTextureHelper: SurfaceTextureHelper

    private var eglBase: EglBase = EglBase.create()
    private var isInitialized = false

    fun initialize() {

        if (isInitialized) return

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        initializeVideo()
        initializeAudio()

        isInitialized = true
    }

    fun getEglBase(): EglBase = eglBase

    private fun initializeVideo() {

        surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)

        videoCapturer = createCameraCapturer()
        videoSource = peerConnectionFactory.createVideoSource(false)

        videoCapturer.initialize(
            surfaceTextureHelper,
            context,
            videoSource.capturerObserver
        )

        videoCapturer.startCapture(720, 1280, 30)

        localVideoTrack =
            peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", videoSource)
        localVideoTrack.setEnabled(true)
    }

    private fun initializeAudio() {
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        audioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO_TRACK", audioSource)
        audioTrack.setEnabled(true)
    }

    // ── Toggle mic ────────────────────────────────────────────────────────────
    fun setMicEnabled(enabled: Boolean) {
        if (isInitialized && ::audioTrack.isInitialized) {
            audioTrack.setEnabled(enabled)
        }
    }

    // ── Toggle camera ─────────────────────────────────────────────────────────
    fun setCameraEnabled(enabled: Boolean) {
        if (isInitialized && ::localVideoTrack.isInitialized) {
            localVideoTrack.setEnabled(enabled)
            if (enabled) {
                videoCapturer.startCapture(720, 1280, 30)
            } else {
                try { videoCapturer.stopCapture() } catch (e: InterruptedException) { e.printStackTrace() }
            }
        }
    }

    // ── Release all resources when leaving room ───────────────────────────────
    fun release() {
        if (!isInitialized) return
        try {
            videoCapturer.stopCapture()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        videoCapturer.dispose()
        videoSource.dispose()
        surfaceTextureHelper.dispose()
        peerConnectionFactory.dispose()
        eglBase.release()
        isInitialized = false
    }

    private fun createCameraCapturer(): VideoCapturer {

        val enumerator = Camera2Enumerator(context)

        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)!!
            }
        }

        throw IllegalStateException("No front camera found")
    }
}