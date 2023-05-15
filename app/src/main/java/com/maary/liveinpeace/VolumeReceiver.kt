package com.maary.liveinpeace

import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

abstract class VolumeReceiver : BroadcastReceiver() {
    private val DEBOUNCE_TIME_MS = 500 // 500ms
    private var lastUpdateTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION" ||
            intent.action.equals(Intent.ACTION_HEADSET_PLUG) ||
            intent.action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        ) {
            if (intent.action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                Log.v("MUTE_", "BLEHEADSET")
                val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_CONNECTED)
                if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    Log.v("MUTE_", "DISCONNECT BLEHEADSET")

                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val deviceType = audioManager.communicationDevice?.type
                    Log.v("MUTE_", deviceType.toString())
                    if (deviceType == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        deviceType == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        deviceType == AudioDeviceInfo.TYPE_BLE_BROADCAST ||
                        deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO){
                        Log.v("MUTE_", "DELAY")

                        handler.postDelayed({ onReceive(context, intent) }, DEBOUNCE_TIME_MS.toLong())
                    }

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