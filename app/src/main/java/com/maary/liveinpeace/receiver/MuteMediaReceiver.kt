package com.maary.liveinpeace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_MUTE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_CANCEL
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_DECREMENT
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_INCREMENT
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_TOGGLE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_UPDATE
import com.maary.liveinpeace.SleepNotification.handle
import com.maary.liveinpeace.SleepNotification.toggle

class MuteMediaReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == BROADCAST_ACTION_MUTE){
            val audioManager = p0?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            do {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
            } while (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0)

            val attributes = AudioAttributes.Builder().setUsage(USAGE_MEDIA).setContentType(CONTENT_TYPE_MUSIC).build()
            val focusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener {}.build()
            audioManager.requestAudioFocus(focusRequest)
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