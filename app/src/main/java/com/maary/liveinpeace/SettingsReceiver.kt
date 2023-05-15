package com.maary.liveinpeace

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.maary.liveinpeace.Constants.Companion.ACTION_CANCEL
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SET_IMG
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SET_NUM
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_SETTINGS
import com.maary.liveinpeace.Constants.Companion.MODE_IMG
import com.maary.liveinpeace.Constants.Companion.MODE_NUM
import com.maary.liveinpeace.Constants.Companion.PREF_ICON
import com.maary.liveinpeace.Constants.Companion.SHARED_PREF

class SettingsReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (ACTION_NAME_SETTINGS == p1?.action){

            val actionImgIcon = p0?.let {
                generateAction(
                    it,
                    SettingsReceiver::class.java,
                    ACTION_NAME_SET_IMG,
                    R.string.icon_type_img
                )
            }

            val actionNumIcon = p0?.let {
                generateAction(
                    it,
                    SettingsReceiver::class.java,
                    ACTION_NAME_SET_NUM,
                    R.string.icon_type_num
                )
            }

            val actionCancel = p0?.let {
                generateAction(
                    it,
                    SettingsReceiver::class.java,
                    ACTION_CANCEL,
                    R.string.cancel
                )
            }

            val actions: MutableList<NotificationCompat.Action> = ArrayList()
            actionImgIcon?.let { actions.add(it) }
            actionNumIcon?.let { actions.add(it) }
            actionCancel?.let { actions.add(it) }

            p0?.let { notify(it, actions) }
        }

        if (ACTION_NAME_SET_IMG == p1?.action){
            val sharedPreferences = p0?.getSharedPreferences(
                SHARED_PREF,
                Context.MODE_PRIVATE
            )
            if (sharedPreferences != null) {
                with(sharedPreferences.edit()){
                    putInt(PREF_ICON, MODE_IMG)
                    apply()
                    val notificationManager: NotificationManager =
                        p0.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val foregroundServiceIntent = Intent(p0, ForegroundService::class.java)
                    p0.stopService(foregroundServiceIntent)
                    p0.startForegroundService(foregroundServiceIntent)
                    notificationManager.cancel(ID_NOTIFICATION_SETTINGS)
                }
            }
        }

        if (ACTION_NAME_SET_NUM == p1?.action){
            val sharedPreferences = p0?.getSharedPreferences(
                SHARED_PREF,
                Context.MODE_PRIVATE
            )
            if (sharedPreferences != null) {
                with(sharedPreferences.edit()){
                    putInt(PREF_ICON, MODE_NUM)
                    apply()
                    val notificationManager: NotificationManager =
                        p0.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val foregroundServiceIntent = Intent(p0, ForegroundService::class.java)
                    p0.stopService(foregroundServiceIntent)
                    p0.startForegroundService(foregroundServiceIntent)
                    notificationManager.cancel(ID_NOTIFICATION_SETTINGS)

                }
            }
        }

        if (ACTION_CANCEL == p1?.action){
            val notificationManager: NotificationManager =
                p0?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(ID_NOTIFICATION_SETTINGS)
        }

    }


    private fun generateAction(
        context: Context,
        targetClass: Class<*>,
        actionName: String,
        actionText: Int

    ): NotificationCompat.Action {
        val intent = Intent(context, targetClass).apply {
            action = actionName
        }

        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_settings_24,
            context.getString(actionText),
            pendingIntent
        ).build()
    }

    private fun notify(
        context: Context,
        actions: List<NotificationCompat.Action>
    ) {

        val notificationSettings = context.let {
            NotificationCompat.Builder(
                it,
                CHANNEL_ID_SETTINGS
            )
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_baseline_settings_24)
                .setShowWhen(false)
                .setContentTitle(context.resources?.getString(R.string.LIP_settings))
                .setOnlyAlertOnce(true)
        }

        for (action in actions) {
            notificationSettings.addAction(action)
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(Constants.ID_NOTIFICATION_SETTINGS, notificationSettings.build())
    }
}