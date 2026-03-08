package com.takehome.twinmind.core.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var stateHolder: RecordingStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var startTimeMs = 0L

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
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startRecording(sessionId: String) {
        startTimeMs = System.currentTimeMillis()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Recording..."),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            },
        )

        val outputDir = filesDir
        audioRecorder.start(outputDir, sessionId, serviceScope)

        stateHolder.updateState {
            copy(isRecording = true, isPaused = false, sessionId = sessionId)
        }

        serviceScope.launch {
            audioRecorder.amplitudes.collectLatest { amp ->
                val elapsed = System.currentTimeMillis() - startTimeMs
                stateHolder.updateState {
                    copy(currentAmplitude = amp, elapsedMs = elapsed)
                }
            }
        }

        serviceScope.launch {
            audioRecorder.chunkReady.collectLatest { chunk ->
                stateHolder.updateState {
                    copy(chunkCount = chunk.chunkIndex + 1)
                }
            }
        }

        Timber.d("RecordingService started for session $sessionId")
    }

    private fun stopRecording() {
        serviceScope.launch {
            audioRecorder.stop()
            stateHolder.updateState {
                copy(isRecording = false, isPaused = false)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

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

    private fun buildNotification(text: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.takehome.twinmind.action.START_RECORDING"
        const val ACTION_STOP = "com.takehome.twinmind.action.STOP_RECORDING"
        const val EXTRA_SESSION_ID = "session_id"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"

        fun startIntent(context: Context, sessionId: String): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
