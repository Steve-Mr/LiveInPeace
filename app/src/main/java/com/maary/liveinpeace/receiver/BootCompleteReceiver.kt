package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.maary.liveinpeace.service.ForegroundService

class BootCompleteReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d("=boot complete=", "Intent.ACTION_BOOT_COMPLETED")
        val intent = Intent(p0, ForegroundService::class.java)
        p0?.startForegroundService(intent)
    }
}