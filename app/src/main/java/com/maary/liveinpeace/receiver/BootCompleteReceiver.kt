package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.maary.liveinpeace.BootWorker

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // 確保 context 不為空，且收到的廣播是正確的
        if (context != null && intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("=boot complete=", "Received boot completed intent, enqueuing worker.")

            // 1. 創建一個一次性的工作請求
            val bootWorkRequest = OneTimeWorkRequestBuilder<BootWorker>().build()

            // 2. 將這個請求加入 WorkManager 的佇列中
            WorkManager.getInstance(context).enqueue(bootWorkRequest)
        }
    }
}