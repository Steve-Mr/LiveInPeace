package com.maary.liveinpeace

class Constants {
    companion object {
        // SharedPref 名称
        const val SHARED_PREF = "com.maary.liveinpeace.pref"
        const val PREF_WATCHING_CONNECTING_TIME = "watching_connecting"
        const val PREF_ENABLE_EAR_PROTECTION = "ear_protection_enabled"
        const val PREF_WELCOME_FINISHED = "welcome_finished"
        // SharedPreferences key for service running state
        const val PREF_SERVICE_RUNNING = "service_running_state"
        const val PREF_HIDE_IN_LAUNCHER = "hide_in_launcher"
        const val PREF_EAR_PROTECTION_THRESHOLD_MAX = "ear_protection_max"
        const val PREF_EAR_PROTECTION_THRESHOLD_MIN = "ear_protection_min"
        const val EAR_PROTECTION_LOWER_THRESHOLD = 10
        const val EAR_PROTECTION_UPPER_THRESHOLD = 25
        // 前台通知 id
        const val ID_NOTIFICATION_FOREGROUND = 1
        const val ID_NOTIFICATION_ALERT = 2
        const val ID_NOTIFICATION_PROTECT = 4
        const val ID_NOTIFICATION_SLEEPTIMER = 5
        // 静音广播名称
        const val BROADCAST_ACTION_MUTE = "com.maary.liveinpeace.MUTE_MEDIA"
        const val BROADCAST_ACTION_SLEEPTIMER_CANCEL = "com.maary.liveinpeace.action.CANCEL"
        const val BROADCAST_ACTION_SLEEPTIMER_INCREMENT = "com.maary.liveinpeace.action.INCREMENT"
        const val BROADCAST_ACTION_SLEEPTIMER_DECREMENT = "com.maary.liveinpeace.action.DECREMENT"
        const val BROADCAST_ACTION_SLEEPTIMER_TOGGLE = "com.maary.liveinpeace.sleeptimer.TOGGLE"
        const val BROADCAST_ACTION_SLEEPTIMER_UPDATE = "com.maary.liveinpeace.sleeptimer.UPDATE"
        const val BROADCAST_ACTION_YABN_MUTE = "com.maary.yetanotherbatterynotifier.receiver.SettingsReceiver.dnd"
        // 前台服务状态改变广播
        const val BROADCAST_ACTION_FOREGROUND = "com.maary.liveinpeace.ACTION_FOREGROUND_SERVICE_STATE"
        const val BROADCAST_FOREGROUND_INTENT_EXTRA = "isForegroundServiceRunning"
        // Broadcast action for connection list updates
        const val BROADCAST_ACTION_CONNECTIONS_UPDATE = "com.maary.liveinpeace.CONNECTIONS_UPDATE"
        const val EXTRA_CONNECTIONS_LIST = "com.maary.liveinpeace.extra.CONNECTIONS_LIST"

        // 不同通知频道 ID
        const val CHANNEL_ID_DEFAULT = "LIP_FOREGROUND"
        const val CHANNEL_ID_SETTINGS = "LIP_SETTINGS"
        const val CHANNEL_ID_ALERT = "LIP_ALERT"
        const val CHANNEL_ID_PROTECT = "LIP_PROTECT"
        const val CHANNEL_ID_WELCOME = "LIP_WELCOME"
        const val CHANNEL_ID_SLEEPTIMER = "LIP_SLEEPTIMER"
        // 提醒时间
        const val ALERT_TIME: Long = 2*60*60*1000
        // 延后时间
        const val DEBOUNCE_TIME_MS = 500
        // 不同通知的 GROUP ID
        const val ID_NOTIFICATION_GROUP_FORE = "LIP_notification_group_foreground"
        const val ID_NOTIFICATION_GROUP_ALERTS = "LIP_notification_group_alerts"
        const val ID_NOTIFICATION_GROUP_PROTECT = "LIP_notification_group_protect"
        const val ID_NOTIFICATION_GROUP_SLEEPTIMER = "LIP_notification_group_sleeptimer"
        const val PATTERN_DATE_DATABASE = "yyyy-MM-dd"
        const val PATTERN_DATE_BUTTON = "MM/dd"

        const val YABN_PACKAGE_NAME = "com.maary.yetanotherbatterynotifier"
        const val YABN_MUTE_RECEIVER = "com.maary.yetanotherbatterynotifier.receiver.SettingsReceiver"
    }
}