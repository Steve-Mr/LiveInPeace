package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.maary.liveinpeace.Constants

class MuteMediaReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == Constants.BROADCAST_ACTION_MUTE){
            val audioManager = p0?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            do {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
            } while (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0)
        }
    }
}