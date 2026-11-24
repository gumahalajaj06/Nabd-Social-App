package com.example.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object EthoraConfig {
    const val APP_ID = "6b04934da487fcd823acf82b"
    const val APP_SECRET = "ef2199f468f77889db9dd7ccffbfdb29"
    const val API_ENDPOINT = "https://api.ethora.com/v1"
}

enum class CallType {
    VOICE, VIDEO
}

enum class CallState {
    IDLE, OUTGOING_RINGING, INCOMING_RINGING, ACTIVE, ENDED, MISSED, DECLINED
}

data class CallSession(
    val callId: String = "",
    val chatId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerUsername: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val callType: String = "voice", // "voice" | "video"
    val state: String = "ringing", // "ringing" | "active" | "ended" | "declined" | "missed"
    val roomId: String = "",
    val startedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val durationSec: Int = 0
)

class EthoraCallManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentCall = MutableStateFlow<CallSession?>(null)
    val currentCall: StateFlow<CallSession?> = _currentCall.asStateFlow()

    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isCameraOn = MutableStateFlow(true)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    fun startCall(chatId: String, currentUid: String, currentName: String, currentUsername: String, peerId: String, peerName: String, callType: CallType) {
        scope.launch {
            try {
                val callId = UUID.randomUUID().toString()
                val roomId = "ethora_${chatId}_${System.currentTimeMillis()}"
                val typeStr = if (callType == CallType.VIDEO) "video" else "voice"
                val session = CallSession(
                    callId = callId,
                    chatId = chatId,
                    callerId = currentUid,
                    callerName = currentName,
                    callerUsername = currentUsername,
                    calleeId = peerId,
                    calleeName = peerName,
                    callType = typeStr,
                    state = "ringing",
                    roomId = roomId,
                    startedAt = Timestamp.now()
                )

                db.collection("chats").document(chatId)
                    .collection("calls").document(callId)
                    .set(session)
                    .await()

                _currentCall.value = session
                _callState.value = CallState.OUTGOING_RINGING
                Log.d("EthoraCallManager", "Started $typeStr call $callId with room $roomId (AppID: ${EthoraConfig.APP_ID})")
            } catch (e: Exception) {
                Log.e("EthoraCallManager", "Error starting call", e)
            }
        }
    }

    fun acceptCall(callSession: CallSession) {
        scope.launch {
            try {
                db.collection("chats").document(callSession.chatId)
                    .collection("calls").document(callSession.callId)
                    .update(
                        mapOf(
                            "state" to "active",
                            "startedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()

                _currentCall.value = callSession.copy(state = "active")
                _callState.value = CallState.ACTIVE
            } catch (e: Exception) {
                Log.e("EthoraCallManager", "Error accepting call", e)
            }
        }
    }

    fun declineCall(callSession: CallSession) {
        scope.launch {
            try {
                db.collection("chats").document(callSession.chatId)
                    .collection("calls").document(callSession.callId)
                    .update(
                        mapOf(
                            "state" to "declined",
                            "endedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()

                resetCallState()
            } catch (e: Exception) {
                Log.e("EthoraCallManager", "Error declining call", e)
            }
        }
    }

    fun endCall(callSession: CallSession?) {
        if (callSession == null) {
            resetCallState()
            return
        }
        scope.launch {
            try {
                db.collection("chats").document(callSession.chatId)
                    .collection("calls").document(callSession.callId)
                    .update(
                        mapOf(
                            "state" to "ended",
                            "endedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            } catch (e: Exception) {
                Log.e("EthoraCallManager", "Error ending call", e)
            } finally {
                resetCallState()
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleCamera() {
        _isCameraOn.value = !_isCameraOn.value
    }

    fun flipCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    private fun resetCallState() {
        _currentCall.value = null
        _callState.value = CallState.IDLE
        _isMuted.value = false
        _isSpeakerOn.value = true
        _isCameraOn.value = true
    }
}
