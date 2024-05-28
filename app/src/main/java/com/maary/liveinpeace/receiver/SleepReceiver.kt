package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_UPDATE

abstract class SleepReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BROADCAST_ACTION_SLEEPTIMER_UPDATE
        ) {
            updateNotification(context)
        }
    }

    abstract fun updateNotification(context: Context)
}