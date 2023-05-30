package com.maary.liveinpeace

class Constants {
    companion object {
        // Cancel 的 Action
        const val ACTION_CANCEL = "com.maary.liveinpeace.receiver.SettingsReceiver.Cancel"
        // 使用字符式图标
        const val MODE_NUM = 0
        // 使用图像式图标
        const val MODE_IMG = 1
        // SharedPref 名称
        const val SHARED_PREF = "com.maary.liveinpeace.pref"
        // 图标类型的 SharedPref 项目名称
        const val PREF_ICON =  "icon_type"
        const val PREF_WATCHING_CONNECTING_TIME = "watching_connecting"
        // 设置通知 id
        const val ID_NOTIFICATION_SETTINGS = 3
        // 前台通知 id
        const val ID_NOTIFICATION_FOREGROUND = 1
        const val ID_NOTIFICATION_ALERT = 2
        // 设置图像式图标 Action
        const val ACTION_NAME_SET_IMG = "com.maary.liveinpeace.receiver.SettingsReceiver.SetIconImg"
        // 设置字符式图标 Action
        const val ACTION_NAME_SET_NUM = "com.maary.liveinpeace.receiver.SettingsReceiver.SetIconNum"
        // 启用长时间连接提醒 Action
        const val ACTION_ENABLE_WATCHING = "com.maary.liveinpeace.receiver.SettingsReceiver.EnableWatching"
        // 禁用长时间连接提醒 Action
        const val ACTION_DISABLE_WATCHING = "com.maary.liveinpeace.receiver.SettingsReceiver.DisableWatching"
        // 设置 Action
        const val ACTION_NAME_SETTINGS = "com.maary.liveinpeace.receiver.SettingsReceiver"
        // 静音广播名称
        const val BROADCAST_ACTION_MUTE = "com.maary.liveinpeace.MUTE_MEDIA"
        // 当音量操作动作太过频繁后等待时间
        const val REQUESTING_WAIT_MILLIS = 500
        // 不同通知频道 ID
        const val CHANNEL_ID_DEFAULT = "LIP_FOREGROUND"
        const val CHANNEL_ID_SETTINGS = "LIP_SETTINGS"
        const val CHANNEL_ID_ALERT = "LIP_ALERT"
        // 提醒时间
        const val ALERT_TIME: Long = 2*60*60*1000
        // 延后时间
        const val DEBOUNCE_TIME_MS = 500
        // 不同通知的 GROUP ID
        const val ID_NOTIFICATION_GROUP_FORE = "LIP_notification_group_foreground"
        const val ID_NOTIFICATION_GROUP_SETTINGS = "LIP_notification_group_settings"
        const val ID_NOTIFICATION_GROUP_ALERTS = "LIP_notification_group_alerts"
    }
}