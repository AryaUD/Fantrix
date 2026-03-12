package com.example.fantrix.webrtc

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import org.webrtc.*

/**
 * WebRTCManager — real peer-to-peer signaling via Firebase Realtime Database.
 *
 * RTDB structure:
 * watch_party_signals/{roomId}/
 *   offers/{fromUid}_{toUid}/  { sdp, type }
 *   answers/{fromUid}_{toUid}/ { sdp, type }
 *   ice_candidates/{fromUid}_{toUid}/{pushId}/ { sdp, sdpMid, sdpMLineIndex }
 */
class WebRTCManager(private val context: Context) {

    companion object {
        private const val TAG = "WebRTCManager"
    }

    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localStream: MediaStream? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    // peerId -> PeerConnection
    private val peerConnections = mutableMapOf<String, PeerConnection>()

    // peerId -> remote VideoTrack
    private val remoteVideoTracks = mutableMapOf<String, VideoTrack>()

    var onRemoteTrackAdded: ((peerId: String, videoTrack: VideoTrack) -> Unit)? = null
    var onRemoteTrackRemoved: ((peerId: String) -> Unit)? = null

    private var database: FirebaseDatabase? = null
    private var roomId: String = ""
    private var localUserId: String = ""

    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        database = FirebaseDatabase.getInstance()
        startLocalCapture()
    }

    private fun startLocalCapture() {
        val factory = peerConnectionFactory ?: return
        val cameraEnumerator = Camera2Enumerator(context)
        val frontCamera = cameraEnumerator.deviceNames
            .firstOrNull { cameraEnumerator.isFrontFacing(it) }
            ?: cameraEnumerator.deviceNames.firstOrNull()
            ?: return

        videoCapturer = cameraEnumerator.createCapturer(frontCamera, null)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)

        val videoSource = factory.createVideoSource(false)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer?.startCapture(640, 480, 30)
        localVideoTrack = factory.createVideoTrack("video_local", videoSource).also { it.setEnabled(true) }

        val audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("audio_local", audioSource).also { it.setEnabled(true) }

        localStream = factory.createLocalMediaStream("stream_local").apply {
            addTrack(localVideoTrack)
            addTrack(localAudioTrack)
        }
    }

    /**
     * Call after joining a room.
     * existingPeerIds = all userIds already in the room (excluding self).
     * The newly joined user sends offers to everyone already there.
     */
    fun joinRoom(roomId: String, localUserId: String, existingPeerIds: List<String>) {
        this.roomId = roomId
        this.localUserId = localUserId

        listenForOffers()
        listenForAnswers()
        listenForIceCandidates()

        // Send offers to everyone already in the room
        existingPeerIds.filter { it != localUserId }.forEach { peerId ->
            createOfferFor(peerId)
        }
    }

    /** Call when a new peer joins after you — they will send you an offer, you answer. */
    fun onNewPeerJoined(peerId: String) {
        // They will initiate the offer to us; we just make sure PC exists
        getOrCreatePeerConnection(peerId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Offer / Answer / ICE
    // ─────────────────────────────────────────────────────────────────────────

    private fun createOfferFor(peerId: String) {
        val pc = getOrCreatePeerConnection(peerId) ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                pc.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        val key = "${localUserId}_$peerId"
                        database?.getReference("watch_party_signals/$roomId/offers/$key")
                            ?.setValue(mapOf("sdp" to sdp.description, "type" to "offer"))
                        Log.d(TAG, "Offer sent -> $peerId")
                    }
                }, sdp)
            }
        }, constraints)
    }

    private fun listenForOffers() {
        val ref = database?.getReference("watch_party_signals/$roomId/offers") ?: return
        ref.addChildEventListener(object : SimpleChildEventListener() {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key ?: return
                // key = {fromUid}_{toUid}  — toUid is the last segment
                val lastUnderscore = key.lastIndexOf('_')
                if (lastUnderscore < 0) return
                val toUid = key.substring(lastUnderscore + 1)
                val fromUid = key.substring(0, lastUnderscore)
                if (toUid != localUserId) return

                val sdpStr = snapshot.child("sdp").getValue(String::class.java) ?: return
                val pc = getOrCreatePeerConnection(fromUid) ?: return
                pc.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        val constraints = MediaConstraints().apply {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                        }
                        pc.createAnswer(object : SimpleSdpObserver() {
                            override fun onCreateSuccess(answerSdp: SessionDescription?) {
                                answerSdp ?: return
                                pc.setLocalDescription(object : SimpleSdpObserver() {
                                    override fun onSetSuccess() {
                                        val answerKey = "${localUserId}_$fromUid"
                                        database?.getReference("watch_party_signals/$roomId/answers/$answerKey")
                                            ?.setValue(mapOf("sdp" to answerSdp.description, "type" to "answer"))
                                        Log.d(TAG, "Answer sent -> $fromUid")
                                    }
                                }, answerSdp)
                            }
                        }, constraints)
                    }
                }, SessionDescription(SessionDescription.Type.OFFER, sdpStr))
            }
        })
    }

    private fun listenForAnswers() {
        val ref = database?.getReference("watch_party_signals/$roomId/answers") ?: return
        ref.addChildEventListener(object : SimpleChildEventListener() {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key ?: return
                val lastUnderscore = key.lastIndexOf('_')
                if (lastUnderscore < 0) return
                val toUid = key.substring(lastUnderscore + 1)
                val fromUid = key.substring(0, lastUnderscore)
                if (toUid != localUserId) return

                val sdpStr = snapshot.child("sdp").getValue(String::class.java) ?: return
                val pc = peerConnections[fromUid] ?: return
                pc.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() { Log.d(TAG, "Answer applied from $fromUid") }
                }, SessionDescription(SessionDescription.Type.ANSWER, sdpStr))
            }
        })
    }

    private fun listenForIceCandidates() {
        val ref = database?.getReference("watch_party_signals/$roomId/ice_candidates") ?: return
        ref.addChildEventListener(object : SimpleChildEventListener() {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleIceSnapshot(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleIceSnapshot(snapshot)
            }
        })
    }

    private fun handleIceSnapshot(snapshot: DataSnapshot) {
        val key = snapshot.key ?: return
        val lastUnderscore = key.lastIndexOf('_')
        if (lastUnderscore < 0) return
        val toUid = key.substring(lastUnderscore + 1)
        val fromUid = key.substring(0, lastUnderscore)
        if (toUid != localUserId) return

        val pc = peerConnections[fromUid] ?: return
        snapshot.children.forEach { candidateSnap ->
            val sdp = candidateSnap.child("sdp").getValue(String::class.java) ?: return@forEach
            val sdpMid = candidateSnap.child("sdpMid").getValue(String::class.java) ?: return@forEach
            val sdpMLineIndex = candidateSnap.child("sdpMLineIndex").getValue(Int::class.java) ?: return@forEach
            pc.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, sdp))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PeerConnection factory
    // ─────────────────────────────────────────────────────────────────────────

    private fun getOrCreatePeerConnection(peerId: String): PeerConnection? {
        peerConnections[peerId]?.let { return it }

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val pc = peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val key = "${localUserId}_$peerId"
                database?.getReference("watch_party_signals/$roomId/ice_candidates/$key")
                    ?.push()
                    ?.setValue(mapOf(
                        "sdp" to candidate.sdp,
                        "sdpMid" to candidate.sdpMid,
                        "sdpMLineIndex" to candidate.sdpMLineIndex
                    ))
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track() ?: return
                if (track is VideoTrack) {
                    remoteVideoTracks[peerId] = track
                    onRemoteTrackAdded?.invoke(peerId, track)
                    Log.d(TAG, "Remote video track from $peerId")
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "[$peerId] connection: $newState")
                if (newState == PeerConnection.PeerConnectionState.DISCONNECTED ||
                    newState == PeerConnection.PeerConnectionState.FAILED) {
                    onRemoteTrackRemoved?.invoke(peerId)
                }
            }

            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        }) ?: return null

        // Add local tracks
        localStream?.audioTracks?.forEach { pc.addTrack(it, listOf("local_stream")) }
        localStream?.videoTracks?.forEach { pc.addTrack(it, listOf("local_stream")) }

        peerConnections[peerId] = pc
        return pc
    }

    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack

    fun setMicEnabled(enabled: Boolean) { localAudioTrack?.setEnabled(enabled) }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        if (enabled) videoCapturer?.startCapture(640, 480, 30) else videoCapturer?.stopCapture()
    }

    fun removePeer(peerId: String) {
        peerConnections[peerId]?.close()
        peerConnections.remove(peerId)
        remoteVideoTracks.remove(peerId)
        onRemoteTrackRemoved?.invoke(peerId)
        cleanSignalingFor(peerId)
    }

    private fun cleanSignalingFor(peerId: String) {
        val base = "watch_party_signals/$roomId"
        listOf("offers", "answers", "ice_candidates").forEach { node ->
            database?.getReference("$base/$node/${localUserId}_$peerId")?.removeValue()
            database?.getReference("$base/$node/${peerId}_$localUserId")?.removeValue()
        }
    }

    fun release() {
        if (roomId.isNotEmpty() && localUserId.isNotEmpty()) {
            // Clean up all my signaling data
            peerConnections.keys.toList().forEach { cleanSignalingFor(it) }
        }
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        remoteVideoTracks.clear()
        try { videoCapturer?.stopCapture() } catch (_: Exception) {}
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        localStream?.dispose()
        peerConnectionFactory?.dispose()
        eglBase.release()
    }
}

// ── Boilerplate reducers ──────────────────────────────────────────────────────

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}

open class SimpleChildEventListener : ChildEventListener {
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
    override fun onChildRemoved(snapshot: DataSnapshot) {}
    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
    override fun onCancelled(error: DatabaseError) {}
}