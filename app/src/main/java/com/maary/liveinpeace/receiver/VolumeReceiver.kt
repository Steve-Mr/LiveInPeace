package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.maary.liveinpeace.Constants.Companion.DEBOUNCE_TIME_MS

abstract class VolumeReceiver : BroadcastReceiver() {
    private var lastUpdateTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION"
        ) {
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime >= DEBOUNCE_TIME_MS) {
                lastUpdateTime = now
                updateNotification(context)
            } else {
                handler.postDelayed({ onReceive(context, intent) }, DEBOUNCE_TIME_MS.toLong())
            }
        }
    }

    abstract fun updateNotification(context: Context)
}