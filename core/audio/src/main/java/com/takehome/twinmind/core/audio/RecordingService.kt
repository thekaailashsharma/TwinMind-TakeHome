package com.takehome.twinmind.core.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.StatFs
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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

    private var audioDeviceCallback: AudioDeviceCallback? = null

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val plugged = intent.getIntExtra("state", 0) == 1
                    val name = if (plugged) "Wired headset connected" else "Wired headset disconnected"
                    Timber.d("Headset event: $name")
                    notifyMicSourceChanged(name)
                }
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)
                    val name = when (state) {
                        BluetoothProfile.STATE_CONNECTED -> "Bluetooth audio connected"
                        BluetoothProfile.STATE_DISCONNECTED -> "Bluetooth audio disconnected"
                        else -> null
                    }
                    if (name != null) {
                        Timber.d("Bluetooth event: $name")
                        notifyMicSourceChanged(name)
                    }
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
        unregisterMicSourceDetection()
        unregisterPhoneCallDetection()
        abandonAudioFocus()
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val sessionId = stateHolder.state.value.sessionId
        if (sessionId != null && stateHolder.state.value.isRecording) {
            Timber.w("Process death detected for session %s — enqueuing termination worker", sessionId)
            serviceScope.launch {
                audioRecorder.stop()
            }
            val data = Data.Builder()
                .putString("session_id", sessionId)
                .build()
            val request = OneTimeWorkRequestBuilder<SessionTerminationWorker>()
                .setInputData(data)
                .addTag("termination_$sessionId")
                .build()
            WorkManager.getInstance(applicationContext).enqueue(request)
        }
    }

    private fun startRecording(sessionId: String) {
        startTimeMs = System.currentTimeMillis()
        totalPauseDurationMs = 0L
        pauseStartMs = 0L

        try {
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
        } catch (e: SecurityException) {
            Timber.e(e, "Cannot start foreground service - microphone permission not granted")
            stateHolder.updateState { copy(isRecording = false) }
            stopSelf()
            return
        }

        if (!hasEnoughStorage()) {
            Timber.w("Low storage - cannot start recording")
            stateHolder.updateState {
                copy(isRecording = false, errorMessage = "Recording stopped - Low storage")
            }
            // Update notification to reflect the error state before stopping.
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(
                NOTIFICATION_ID,
                buildNotification("Recording stopped - Low storage", isPaused = false),
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val outputDir = filesDir
        audioRecorder.start(outputDir, sessionId, serviceScope)

        stateHolder.updateState {
            copy(
                isRecording = true,
                isPaused = false,
                pauseReason = null,
                sessionId = sessionId,
                silenceDetected = false,
                errorMessage = null,
                micSourceChanged = null,
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
                        stateHolder.updateState {
                            copy(errorMessage = "Recording stopped - Low storage")
                        }
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
        registerMicSourceDetection()

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
            unregisterMicSourceDetection()
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

    // --- Microphone source detection ---

    private fun registerMicSourceDetection() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(headsetReceiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    for (d in addedDevices) {
                        if (d.isSource) {
                            notifyMicSourceChanged("Microphone added: ${d.productName}")
                        }
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    for (d in removedDevices) {
                        if (d.isSource) {
                            notifyMicSourceChanged("Microphone removed: ${d.productName}")
                        }
                    }
                }
            }
            am.registerAudioDeviceCallback(audioDeviceCallback, null)
        }
    }

    private fun unregisterMicSourceDetection() {
        try {
            unregisterReceiver(headsetReceiver)
        } catch (_: IllegalArgumentException) { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback?.let {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                am.unregisterAudioDeviceCallback(it)
            }
            audioDeviceCallback = null
        }
    }

    private fun notifyMicSourceChanged(description: String) {
        stateHolder.updateState { copy(micSourceChanged = description) }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification("Recording - $description", isPaused = false))
        serviceScope.launch {
            delay(3000)
            stateHolder.updateState { copy(micSourceChanged = null) }
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

    @Suppress("NewApi")
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

        if (Build.VERSION.SDK_INT >= 36) {
            return buildAndroid16Notification(text, isPaused, pendingIntent, stopPendingIntent)
        }

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

    @Suppress("NewApi")
    private fun buildAndroid16Notification(
        text: String,
        isPaused: Boolean,
        contentIntent: PendingIntent,
        stopIntent: PendingIntent,
    ): Notification {
        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)

        if (isPaused) {
            progressStyle.setProgressIndeterminate(false)
            progressStyle.addProgressSegment(
                Notification.ProgressStyle.Segment(100).setColor(android.graphics.Color.GRAY),
            )
            progressStyle.setProgress(0)
        } else {
            progressStyle.setProgressIndeterminate(true)
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setStyle(progressStyle)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop",
                    stopIntent,
                ).build(),
            )

        if (isPaused) {
            val resumePendingIntent = PendingIntent.getService(
                this,
                2,
                resumeIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "Resume",
                    resumePendingIntent,
                ).build(),
            )
        } else {
            val pausePendingIntent = PendingIntent.getService(
                this,
                3,
                pauseIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "Pause",
                    pausePendingIntent,
                ).build(),
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
