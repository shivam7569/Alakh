package com.andy.alakh.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.andy.alakh.R
import com.andy.alakh.shared.model.WorkoutType
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the [ExerciseRepository] for the duration of a
 * workout so Health Services keeps delivering data while the screen is off.
 *
 * foregroundServiceType (declared in the manifest) must stay in sync with the
 * FOREGROUND_SERVICE_* permissions, or Android 14 ends the exercise with
 * AUTO_ENDED_PERMISSION_LOST.
 */
class ExerciseService : LifecycleService() {

    private val repository by lazy { ExerciseRepository.getInstance(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startWorkout(intent.getStringExtra(EXTRA_TYPE))
            ACTION_STOP -> stopWorkout()
        }
        return START_NOT_STICKY
    }

    private fun startWorkout(typeName: String?) {
        startAsForeground()
        val type = runCatching { WorkoutType.valueOf(typeName.orEmpty()) }
            .getOrDefault(WorkoutType.WALKING)
        lifecycleScope.launch { runCatching { repository.start(type) } }
    }

    private fun stopWorkout() {
        lifecycleScope.launch {
            runCatching { repository.stop() }
            ServiceCompat.stopForeground(this@ExerciseService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Workout", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alakh")
            .setContentText("Workout in progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, notification, serviceType)
        // TODO: attach a Wear OngoingActivity (androidx.wear:wear-ongoing) so the
        //       workout is glanceable from the watch face.
    }

    companion object {
        const val ACTION_START = "com.andy.alakh.action.START"
        const val ACTION_STOP = "com.andy.alakh.action.STOP"
        const val EXTRA_TYPE = "com.andy.alakh.extra.TYPE"

        private const val CHANNEL_ID = "alakh_workout"
        private const val NOTIF_ID = 1001
    }
}
