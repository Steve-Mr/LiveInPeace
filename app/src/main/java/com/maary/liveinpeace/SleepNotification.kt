package com.maary.liveinpeace

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_MUTE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_CANCEL
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_DECREMENT
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_INCREMENT
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_YABN_MUTE
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_SLEEPTIMER
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_SLEEPTIMER
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_SLEEPTIMER
import com.maary.liveinpeace.Constants.Companion.YABN_MUTE_RECEIVER
import com.maary.liveinpeace.Constants.Companion.YABN_PACKAGE_NAME
import com.maary.liveinpeace.SleepNotification.Action.CANCEL
import com.maary.liveinpeace.SleepNotification.Action.DECREMENT
import com.maary.liveinpeace.SleepNotification.Action.INCREMENT
import com.maary.liveinpeace.receiver.MuteMediaReceiver
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit


object SleepNotification {

    private val TIMEOUT_INITIAL_MILLIS = TimeUnit.MINUTES.toMillis(30)
    private val TIMEOUT_INCREMENT_MILLIS = TimeUnit.MINUTES.toMillis(10)
    private val TIMEOUT_DECREMENT_MILLIS = TimeUnit.MINUTES.toMillis(10)

    private enum class Action(private val value: String) {
        CANCEL(BROADCAST_ACTION_SLEEPTIMER_CANCEL) {
            override fun title(context: Context) = context.getText(android.R.string.cancel)
        },
        INCREMENT(BROADCAST_ACTION_SLEEPTIMER_INCREMENT) {
            override fun title(context: Context) = "+" + TimeUnit.MILLISECONDS.toMinutes(TIMEOUT_INCREMENT_MILLIS)
        },
        DECREMENT(BROADCAST_ACTION_SLEEPTIMER_DECREMENT) {
            override fun title(context: Context) = "-" + TimeUnit.MILLISECONDS.toMinutes(TIMEOUT_DECREMENT_MILLIS)
        },
        ;

        companion object {
            fun parse(value: String?): Action? = entries.firstOrNull { it.value == value }
        }

        fun intent(context: Context): Intent = Intent(context, MuteMediaReceiver::class.java).setAction(value)

        fun pendingIntent(context: Context, cancel: Boolean = false): PendingIntent? =
            PendingIntent.getBroadcast(context, 0, intent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT ).apply { if (cancel) cancel() }

        fun action(context: Context, cancel: Boolean = false): Notification.Action.Builder =
            Notification.Action.Builder(Icon.createWithResource(context, 0), title(context), pendingIntent(context, cancel))

        abstract fun title(context: Context): CharSequence?
    }

    fun Context.notificationManager(): NotificationManager? = getSystemService(NotificationManager::class.java)

    fun Context.find() = notificationManager()?.activeNotifications?.firstOrNull { it.id == ID_NOTIFICATION_SLEEPTIMER }?.notification

    fun Context.handle(intent: Intent?) = when (Action.parse(intent?.action)) {
        INCREMENT -> update(TIMEOUT_INCREMENT_MILLIS)
        DECREMENT -> update(-TIMEOUT_DECREMENT_MILLIS)
        CANCEL -> cancel()
        null -> Unit
    }

    fun Context.toggle() = if (find() == null) show() else cancel()

    private fun Context.cancel() = notificationManager()?.cancel(ID_NOTIFICATION_SLEEPTIMER) ?: Unit

    private fun Context.update(timeout: Long) = find()?.let { it.`when` - System.currentTimeMillis() }?.let { if (it > -timeout) it + timeout else it }?.let { show(it) }

    private fun Context.show(timeout: Long = TIMEOUT_INITIAL_MILLIS) {
        require(timeout > 0)
        val eta = System.currentTimeMillis() + timeout

        val muteYABNIntent = Intent()
        muteYABNIntent.setComponent(
            ComponentName(
                YABN_PACKAGE_NAME,
                YABN_MUTE_RECEIVER
            )
        )
        muteYABNIntent.action = BROADCAST_ACTION_YABN_MUTE
        sendBroadcast(muteYABNIntent)

        val muteMediaIntent = Intent(this, MuteMediaReceiver::class.java)
        muteMediaIntent.action = BROADCAST_ACTION_MUTE
        val pendingMuteIntent = PendingIntent.getBroadcast(this, 0, muteMediaIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)


        val notification = Notification.Builder(this, CHANNEL_ID_SLEEPTIMER)
            .setCategory(Notification.CATEGORY_EVENT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_tile)
            .setSubText(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(eta)))
            .setShowWhen(true).setWhen(eta)
            .setGroup(ID_NOTIFICATION_GROUP_SLEEPTIMER)
            .setUsesChronometer(true).setChronometerCountDown(true)
            .setTimeoutAfter(timeout)
            .setDeleteIntent(pendingMuteIntent)
            .addAction(INCREMENT.action(this).build())
            .addAction(DECREMENT.action(this, cancel = timeout <= TIMEOUT_DECREMENT_MILLIS).build())
            .addAction(CANCEL.action(this).build())
            .build()
        notificationManager()?.notify(ID_NOTIFICATION_SLEEPTIMER, notification)
    }

}