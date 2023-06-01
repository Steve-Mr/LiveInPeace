package com.maary.liveinpeace

import com.maary.liveinpeace.database.Connection

interface DeviceMapChangeListener {
    fun onDeviceMapChanged(deviceMap: Map<String, Connection>)
}
