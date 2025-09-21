package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maary.liveinpeace.Constants.Companion.THROTTLE_TIME_MS

abstract class VolumeReceiver : BroadcastReceiver() {
    private var lastUpdateTime: Long = 0

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime >= THROTTLE_TIME_MS) {
                lastUpdateTime = now
                updateNotification(context)
            }
        }
    }

    abstract fun updateNotification(context: Context)
}