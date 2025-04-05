package com.maary.liveinpeace.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.sql.Date

@Parcelize
@Entity(tableName = "connection_table")
data class Connection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "connected_time") val connectedTime: Long?,
    @ColumnInfo(name = "disconnected_time") val disconnectedTime: Long?,
    @ColumnInfo(name = "duration") val duration: Long?,
    @ColumnInfo(name = "date") val date: String,
    ) : Parcelable
