package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_MUTE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_CANCEL
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_DECREMENT
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_INCREMENT
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_TOGGLE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_UPDATE
import com.maary.liveinpeace.SleepNotification.handle
import com.maary.liveinpeace.SleepNotification.toggle
import com.maary.liveinpeace.service.ForegroundService

class MuteMediaReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == BROADCAST_ACTION_MUTE){
            Log.d("MuteMediaReceiver", "BROADCAST_ACTION_MUTE received. Starting ForegroundService to handle it.")
            p0?.let { context ->
                val intent = Intent(context, ForegroundService::class.java).apply {
                    action = ForegroundService.ACTION_MUTE_MEDIA
                }
                context.startService(intent)
            }
        }

        if (p1?.action == BROADCAST_ACTION_SLEEPTIMER_CANCEL ||
            p1?.action == BROADCAST_ACTION_SLEEPTIMER_INCREMENT ||
            p1?.action == BROADCAST_ACTION_SLEEPTIMER_DECREMENT) {
            p0?.handle(p1)
            val intent = Intent(BROADCAST_ACTION_SLEEPTIMER_UPDATE)
            p0?.sendBroadcast(intent)
        }

        if (p1?.action == BROADCAST_ACTION_SLEEPTIMER_TOGGLE) {
            p0?.toggle()
            val intent = Intent(BROADCAST_ACTION_SLEEPTIMER_UPDATE)
            p0?.sendBroadcast(intent)
        }
    }
}