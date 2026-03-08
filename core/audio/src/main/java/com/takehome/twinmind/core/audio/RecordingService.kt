package com.takehome.twinmind.core.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.StatFs
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var stateHolder: RecordingStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var startTimeMs = 0L
    private var totalPauseDurationMs = 0L
    private var pauseStartMs = 0L
    private var timerJob: Job? = null

    private var telephonyCallbackRef: Any? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: android.telephony.PhoneStateListener? = null

    private var audioFocusRequestRef: Any? = null
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (!stateHolder.state.value.isPaused) {
                    pauseRecording("Paused - Audio focus lost")
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                val state = stateHolder.state.value
                if (state.isPaused && state.pauseReason == "Paused - Audio focus lost") {
                    resumeRecording()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_NOT_STICKY
                startRecording(sessionId)
            }
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording("Paused by user")
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterPhoneCallDetection()
        abandonAudioFocus()
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startRecording(sessionId: String) {
        startTimeMs = System.currentTimeMillis()
        totalPauseDurationMs = 0L
        pauseStartMs = 0L

        if (!hasEnoughStorage()) {
            Timber.w("Low storage - cannot start recording")
            stateHolder.updateState { copy(isRecording = false) }
            stopSelf()
            return
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Recording...", isPaused = false),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            },
        )

        val outputDir = filesDir
        audioRecorder.start(outputDir, sessionId, serviceScope)

        stateHolder.updateState {
            copy(
                isRecording = true,
                isPaused = false,
                pauseReason = null,
                sessionId = sessionId,
                silenceDetected = false,
            )
        }

        timerJob = serviceScope.launch {
            while (isActive) {
                val state = stateHolder.state.value
                if (!state.isPaused) {
                    val elapsed = System.currentTimeMillis() - startTimeMs - totalPauseDurationMs
                    stateHolder.updateState { copy(elapsedMs = elapsed) }

                    if (!hasEnoughStorage()) {
                        Timber.w("Low storage detected during recording")
                        stopRecording()
                        return@launch
                    }
                }

                val elapsed = stateHolder.state.value.elapsedMs
                val statusText = if (state.isPaused) {
                    state.pauseReason ?: "Paused"
                } else {
                    "Recording - ${formatTime(elapsed)}"
                }

                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(statusText, state.isPaused))
                delay(1000)
            }
        }

        serviceScope.launch {
            audioRecorder.amplitudes.collectLatest { amp ->
                stateHolder.updateState { copy(currentAmplitude = amp) }
            }
        }

        serviceScope.launch {
            audioRecorder.chunkReady.collectLatest { chunk ->
                stateHolder.updateState { copy(chunkCount = chunk.chunkIndex + 1) }
            }
        }

        serviceScope.launch {
            audioRecorder.silenceDetected.collectLatest { silent ->
                stateHolder.updateState { copy(silenceDetected = silent) }
            }
        }

        registerPhoneCallDetection()
        requestAudioFocus()

        Timber.d("RecordingService started for session $sessionId")
    }

    private fun pauseRecording(reason: String) {
        pauseStartMs = System.currentTimeMillis()
        audioRecorder.pause()
        stateHolder.updateState {
            copy(isPaused = true, pauseReason = reason)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(reason, isPaused = true))
        Timber.d("Recording paused: $reason")
    }

    private fun resumeRecording() {
        if (pauseStartMs > 0) {
            totalPauseDurationMs += System.currentTimeMillis() - pauseStartMs
            pauseStartMs = 0
        }
        audioRecorder.resume()
        stateHolder.updateState {
            copy(isPaused = false, pauseReason = null, silenceDetected = false)
        }
        Timber.d("Recording resumed")
    }

    private fun stopRecording() {
        serviceScope.launch {
            timerJob?.cancel()
            unregisterPhoneCallDetection()
            abandonAudioFocus()
            audioRecorder.stop()
            stateHolder.updateState {
                copy(
                    isRecording = false,
                    isPaused = false,
                    pauseReason = null,
                    silenceDetected = false,
                )
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // --- Phone call detection ---

    private fun registerPhoneCallDetection() {
        val telephonyManager =
            getSystemService(TELEPHONY_SERVICE) as? TelephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state)
                }
            }
            try {
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallbackRef = callback
            } catch (e: SecurityException) {
                Timber.w(e, "Cannot register telephony callback")
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : android.telephony.PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state)
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager.listen(
                listener,
                android.telephony.PhoneStateListener.LISTEN_CALL_STATE,
            )
            phoneStateListener = listener
        }
    }

    private fun unregisterPhoneCallDetection() {
        val telephonyManager =
            getSystemService(TELEPHONY_SERVICE) as? TelephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallbackRef as? TelephonyCallback)?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
            telephonyCallbackRef = null
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager.listen(
                    it,
                    android.telephony.PhoneStateListener.LISTEN_NONE,
                )
            }
            phoneStateListener = null
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!stateHolder.state.value.isPaused) {
                    pauseRecording("Paused - Phone call")
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                val currentState = stateHolder.state.value
                if (currentState.isPaused && currentState.pauseReason == "Paused - Phone call") {
                    resumeRecording()
                }
            }
        }
    }

    // --- Audio focus ---

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioManager.requestAudioFocus(request)
            audioFocusRequestRef = request
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (audioFocusRequestRef as? AudioFocusRequest)?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
            audioFocusRequestRef = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    // --- Storage check ---

    private fun hasEnoughStorage(): Boolean {
        val stat = StatFs(filesDir.path)
        return stat.availableBytes > MIN_STORAGE_BYTES
    }

    // --- Notification ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active recording notification"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, isPaused: Boolean): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_delete,
                "Stop",
                stopPendingIntent,
            )

        if (isPaused) {
            val resumePendingIntent = PendingIntent.getService(
                this,
                2,
                resumeIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Resume",
                resumePendingIntent,
            )
        } else {
            val pausePendingIntent = PendingIntent.getService(
                this,
                3,
                pauseIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent,
            )
        }

        return builder.build()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    companion object {
        const val ACTION_START = "com.takehome.twinmind.action.START_RECORDING"
        const val ACTION_STOP = "com.takehome.twinmind.action.STOP_RECORDING"
        const val ACTION_PAUSE = "com.takehome.twinmind.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.takehome.twinmind.action.RESUME_RECORDING"
        const val EXTRA_SESSION_ID = "session_id"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"
        private const val MIN_STORAGE_BYTES = 50L * 1024 * 1024

        fun startIntent(context: Context, sessionId: String): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }

        fun pauseIntent(context: Context): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_PAUSE
            }

        fun resumeIntent(context: Context): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_RESUME
            }
    }
}
