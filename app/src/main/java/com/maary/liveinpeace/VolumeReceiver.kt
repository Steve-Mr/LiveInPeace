package com.maary.liveinpeace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

abstract class VolumeReceiver: BroadcastReceiver() {
    private val DEBOUNCE_TIME_MS = 500 // 500ms
    private var lastUpdateTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION" ||
                intent.action == "android.media.AUDIO_BECOMING_NOISY") {
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