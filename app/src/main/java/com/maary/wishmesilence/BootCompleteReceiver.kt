package com.maary.wishmesilence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompleteReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d("=boot complete=", "Intent.ACTION_BOOT_COMPLETED");
        val intent = Intent(p0, ForegroundService::class.java)
        p0?.startForegroundService(intent)
    }
}