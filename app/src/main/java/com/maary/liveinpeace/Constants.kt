package com.maary.liveinpeace

class Constants {
    companion object {
        // Cancel 的 Action
        const val ACTION_CANCEL = "com.maary.liveinpeace.SettingsReceiver.Cancel"
        // 使用字符式图标
        const val MODE_NUM = 0
        // 使用图像式图标
        const val MODE_IMG = 1
        // SharedPref 名称
        const val SHARED_PREF = "com.maary.liveinpeace.pref"
        // 图标类型的 SharedPref 项目名称
        const val PREF_ICON =  "icon_type"
        // 设置通知 id
        const val ID_NOTIFICATION_SETTINGS = 3
        // 前台通知 id
        const val ID_NOTIFICATION_FOREGROUND = 1
        const val ID_NOTIFICATION_ALERT = 2
        // 设置图像式图标 Action
        const val ACTION_NAME_SET_IMG = "com.maary.liveinpeace.SettingsReceiver.SetIconImg"
        // 设置字符式图标 Action
        const val ACTION_NAME_SET_NUM = "com.maary.liveinpeace.SettingsReceiver.SetIconNum"
        // 设置 Action
        const val ACTION_NAME_SETTINGS = "com.maary.liveinpeace.SettingsReceiver"
        // 静音广播名称
        const val BROADCAST_ACTION_MUTE = "com.maary.liveinpeace.MUTE_MEDIA"
        // 当音量操作动作太过频繁后等待时间
        const val REQUESTING_WAIT_MILLIS = 500
        // 前台通知频道 ID
        const val CHANNEL_ID_DEFAULT = "LIP_FOREGROUND"
        // 设置通知频道 ID
        const val CHANNEL_ID_SETTINGS = "LIP_SETTINGS"
        const val CHANNEL_ID_ALERT = "LIP_ALERT"
        const val ALERT_TIME: Long = 1000//2*60*60*1000
    }
}