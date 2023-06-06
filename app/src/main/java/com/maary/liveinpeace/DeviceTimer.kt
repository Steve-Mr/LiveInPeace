package com.maary.liveinpeace

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class DeviceTimer(private val context: Context, private val deviceName: String) {
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    private val runnable = Runnable {
        with(NotificationManagerCompat.from(context)) {
            notify(Constants.ID_NOTIFICATION_ALERT, createTimerNotification(context = context, deviceName = deviceName))
        }
    }

    fun start() {
        handler.postDelayed(runnable, Constants.ALERT_TIME)
        Log.v("MUTE_TIMER", "TIMER_STARTED")
    }

    fun stop() {
        handler.removeCallbacks(runnable)

    }

    private fun createTimerNotification(context: Context, deviceName: String) : Notification {
        return NotificationCompat.Builder(context, Constants.CHANNEL_ID_ALERT)
            .setContentTitle(context.getString(R.string.alert))
            .setContentText(String.format(
                context.resources.getString(R.string.device_connected_too_long),
                deviceName
            ))
            .setSmallIcon(R.drawable.ic_headphone)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroupSummary(false)
            .setGroup(Constants.ID_NOTIFICATION_GROUP_ALERTS)
            .build()
    }
}