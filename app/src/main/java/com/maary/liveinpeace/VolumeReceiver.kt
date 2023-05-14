package com.maary.liveinpeace

import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

abstract class VolumeReceiver : BroadcastReceiver() {
    private val DEBOUNCE_TIME_MS = 500 // 500ms
    private var lastUpdateTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    private var isHandlingDelayedEvent = false

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION" ||
            intent.action.equals(Intent.ACTION_HEADSET_PLUG) ||
            intent.action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        ) {

            if (intent.action.equals(Intent.ACTION_HEADSET_PLUG)) {
                Log.v("MUTE_", "HEADSET")
            }
            if (intent.action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                Log.v("MUTE_", "BLEHEADSET")
                if (!isHandlingDelayedEvent) {
                    isHandlingDelayedEvent = true
                    handler.postDelayed({
                        // 延迟 500ms 后执行此处代码
                        isHandlingDelayedEvent = false
                        // 处理触发事件的逻辑
                        updateNotification(context)
                    }, DEBOUNCE_TIME_MS.toLong())
                }
            }
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