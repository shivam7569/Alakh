package com.andy.alakh.health

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Runtime permission checks for workout tracking. */
object PermissionHelper {

    // BODY_SENSORS = heart rate, ACCESS_FINE_LOCATION = GPS exercises,
    // POST_NOTIFICATIONS = the foreground-service / ongoing notification.
    val WORKOUT_PERMISSIONS = listOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    // What the strength-workout monitor needs (no GPS): heart rate (BODY_SENSORS), calories
    // (ACTIVITY_RECOGNITION), and the FGS notification (POST_NOTIFICATIONS).
    val SENSOR_PERMISSIONS = listOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    fun missingPermissions(context: Context): List<String> =
        WORKOUT_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun missingSensorPermissions(context: Context): List<String> =
        SENSOR_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    /** Only BODY_SENSORS actually blocks heart rate (POST_NOTIFICATIONS is just for the FGS notice). */
    fun missingBodySensors(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED

    fun hasWorkoutPermissions(context: Context): Boolean =
        missingPermissions(context).isEmpty()
}
