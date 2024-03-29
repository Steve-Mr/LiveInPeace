package com.maary.liveinpeace.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.maary.liveinpeace.Constants.Companion.ACTION_CANCEL
import com.maary.liveinpeace.Constants.Companion.ACTION_DISABLE_WATCHING
import com.maary.liveinpeace.Constants.Companion.ACTION_ENABLE_WATCHING
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SET_IMG
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SET_NUM
import com.maary.liveinpeace.Constants.Companion.ACTION_TOGGLE_AUTO_CONNECTION_ADJUSTMENT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_SETTINGS
import com.maary.liveinpeace.Constants.Companion.MODE_IMG
import com.maary.liveinpeace.Constants.Companion.MODE_NUM
import com.maary.liveinpeace.Constants.Companion.PREF_ENABLE_EAR_PROTECTION
import com.maary.liveinpeace.Constants.Companion.PREF_ICON
import com.maary.liveinpeace.Constants.Companion.PREF_WATCHING_CONNECTING_TIME
import com.maary.liveinpeace.Constants.Companion.SHARED_PREF
import com.maary.liveinpeace.R
import com.maary.liveinpeace.service.ForegroundService

class SettingsReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (ACTION_NAME_SETTINGS == p1?.action){

            val sharedPref = p0?.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)

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

            val actionEnableWatching = p0?.let {
                generateAction(
                    it,
                    SettingsReceiver::class.java,
                    ACTION_ENABLE_WATCHING,
                    R.string.enable_watching
                )
            }

            val actionDisableWatching = p0?.let {
                generateAction(
                    it,
                    SettingsReceiver::class.java,
                    ACTION_DISABLE_WATCHING,
                    R.string.disable_watching
                )
            }

            val actions: MutableList<NotificationCompat.Action> = ArrayList()
            if (sharedPref!!.getInt(PREF_ICON, MODE_IMG) == MODE_NUM){
                actionImgIcon?.let { actions.add(it) }
            }else {
                actionNumIcon?.let { actions.add(it) }
            }
            if (sharedPref.getBoolean(PREF_WATCHING_CONNECTING_TIME, false)){
                actionDisableWatching?.let { actions.add(it) }
            }else {
                actionEnableWatching?.let { actions.add(it) }
            }
            actionCancel?.let { actions.add(it) }

            notify(p0, actions)
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

        if (ACTION_ENABLE_WATCHING == p1?.action){
            val sharedPreferences = p0?.getSharedPreferences(
                SHARED_PREF,
                Context.MODE_PRIVATE
            )
            if (sharedPreferences != null) {
                with(sharedPreferences.edit()){
                    putBoolean(PREF_WATCHING_CONNECTING_TIME, true)
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

        if (ACTION_DISABLE_WATCHING == p1?.action){
            val sharedPreferences = p0?.getSharedPreferences(
                SHARED_PREF,
                Context.MODE_PRIVATE
            )
            if (sharedPreferences != null) {
                with(sharedPreferences.edit()){
                    putBoolean(PREF_WATCHING_CONNECTING_TIME, false)
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

        if (ACTION_TOGGLE_AUTO_CONNECTION_ADJUSTMENT == p1?.action){
            val sharedPreferences = p0?.getSharedPreferences(
                SHARED_PREF,
                Context.MODE_PRIVATE
            )
            if (sharedPreferences != null) {
                with(sharedPreferences.edit()){
                    putBoolean(PREF_ENABLE_EAR_PROTECTION,
                    !sharedPreferences.getBoolean(PREF_ENABLE_EAR_PROTECTION, false)
                    )
                    apply()
                    val foregroundServiceIntent = Intent(p0, ForegroundService::class.java)
                    p0.stopService(foregroundServiceIntent)
                    p0.startForegroundService(foregroundServiceIntent)
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
                .setGroupSummary(false)
                .setGroup(ID_NOTIFICATION_GROUP_SETTINGS)
        }

        for (action in actions) {
            notificationSettings.addAction(action)
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(ID_NOTIFICATION_SETTINGS, notificationSettings.build())
    }
}