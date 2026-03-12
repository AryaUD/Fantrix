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

    // ✅ Single shared EglBase — must be reused by CameraPreview
    val eglBase: EglBase = EglBase.create()

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoDecoderFactory(
                org.webrtc.DefaultVideoDecoderFactory(eglBase.eglBaseContext)
            )
            .setVideoEncoderFactory(
                org.webrtc.DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            )
            .createPeerConnectionFactory()

        initializeVideo()
        initializeAudio()

        isInitialized = true
    }

    private fun initializeVideo() {
        surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)

        videoCapturer = createFrontCameraCapturer()
        videoSource = peerConnectionFactory.createVideoSource(false)

        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
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

    fun setMicEnabled(enabled: Boolean) {
        if (isInitialized && ::audioTrack.isInitialized) {
            audioTrack.setEnabled(enabled)
        }
    }

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

    fun release() {
        if (!isInitialized) return
        try { videoCapturer.stopCapture() } catch (e: InterruptedException) { e.printStackTrace() }
        videoCapturer.dispose()
        videoSource.dispose()
        surfaceTextureHelper.dispose()
        peerConnectionFactory.dispose()
        eglBase.release()
        isInitialized = false
    }

    // ✅ Front/selfie camera
    private fun createFrontCameraCapturer(): VideoCapturer {
        val enumerator = Camera2Enumerator(context)

        // First try front-facing camera
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)!!
            }
        }

        // Fallback to any available camera
        for (deviceName in enumerator.deviceNames) {
            return enumerator.createCapturer(deviceName, null)!!
        }

        throw IllegalStateException("No camera found on this device")
    }
}