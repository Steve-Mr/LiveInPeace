package com.maary.liveinpeace

class Constants {
    companion object {
        const val ACTION_CANCEL = "com.maary.liveinpeace.SettingsReceiver.Cancel"
        const val MODE_NUM = 0
        const val MODE_IMG = 1
        const val SHARED_PREF = "com.maary.liveinpeace.pref"
        const val PREF_ICON =  "icon_type"
        const val ID_NOTIFICATION_SETTINGS = 3
        const val ID_NOTIFICATION_FOREGROUND = 1
        const val ACTION_NAME_SET_IMG = "com.maary.liveinpeace.SettingsReceiver.SetIconImg"
        const val ACTION_NAME_SET_NUM = "com.maary.liveinpeace.SettingsReceiver.SetIconNum"
        const val ACTION_NAME_SETTINGS = "com.maary.liveinpeace.SettingsReceiver"
        const val BROADCAST_ACTION_MUTE = "com.maary.liveinpeace.MUTE_MEDIA"
        const val REQUESTING_WAIT_MILLIS = 500
    }
}