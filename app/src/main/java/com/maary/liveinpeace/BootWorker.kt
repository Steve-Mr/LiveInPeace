package com.maary.liveinpeace

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maary.liveinpeace.service.ForegroundService

class BootWorker (
    private val context: Context,
    workerParams: WorkerParameters)
    : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        // 在這裡執行啟動前台服務的邏輯
        // WorkManager 在執行 doWork 時，系統允許應用啟動前台服務
        val intent = Intent(context, ForegroundService::class.java)

        try {
            context.startForegroundService(intent)
            // 任務成功
            return Result.success()
        } catch (e: Exception) {
            // 如果啟動失敗，記錄錯誤並回報失敗
             Log.e("BootWorker", "Failed to start foreground service", e)
            return Result.failure()
        }
    }
}