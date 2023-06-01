package com.maary.liveinpeace

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.media.AudioDeviceInfo
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maary.liveinpeace.database.Connection
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class ConnectionListAdapter : ListAdapter<Connection, ConnectionListAdapter.ConnectionViewHolder>(ConnectionsComparator()){

    private var currentItemLayout = R.layout.item_connection

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        return ConnectionViewHolder.create(parent, currentItemLayout)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind( current.name, current.type, current.duration, current.disconnectedTime)
    }

    class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val connectionIconView: ImageView = itemView.findViewById(R.id.device_icon)
        private val connectionDeviceNameView: TextView = itemView.findViewById(R.id.device_name)
        private val connectionDurationView: TextView = itemView.findViewById(R.id.device_connection_time)
        private val connectionIndicatorView: ImageView = itemView.findViewById(R.id.connection_time_prefix)

        fun bind(deviceName: String?, type: Int?, duration: Long?, disconnectedTime: Long?) {
            connectionIconView.setImageResource(
                chooseDeviceDrawable(
                    type = type,
                    drawableHeadphone = R.drawable.ic_headphone_round,
                    drawableBLE = R.drawable.ic_bluetooth_round))
            connectionDeviceNameView.text = deviceName
            connectionDurationView.text = duration?.let { formatMilliseconds(itemView.context, it) }

            if (disconnectedTime == null){
                // Get the desired tint color from resources
                val tintColor = itemView.context.getColor(android.R.color.holo_green_light)

                // Create a ColorStateList with the desired tint color
                val colorStateList = ColorStateList.valueOf(tintColor)
                connectionIndicatorView.imageTintList = colorStateList
                connectionIconView.setImageResource(
                    chooseDeviceDrawable(
                        type = type,
                        drawableHeadphone = R.drawable.ic_headphone_round_alt,
                        drawableBLE = R.drawable.ic_bluetooth_round_alt))
            }
        }

        companion object {
            fun create(parent: ViewGroup, layoutId: Int): ConnectionViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(layoutId, parent, false)
                return ConnectionViewHolder(view)
            }
        }

        private fun chooseDeviceDrawable(type: Int?, drawableHeadphone: Int, drawableBLE: Int): Int{
            return if (type in listOf(
                    AudioDeviceInfo.TYPE_BLE_BROADCAST,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                )){
                (drawableBLE)
            } else {
                (drawableHeadphone)
            }
        }

        private fun formatMilliseconds(context: Context, milliseconds: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
            return if (hours == 0L){
                String.format(context.getString(R.string.duration_minutes), minutes)
            }else {
                String.format(context.getString(R.string.duration_hour), hours, minutes)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItemLayout(newItemLayout: Int) {
        currentItemLayout = newItemLayout
        notifyDataSetChanged()
    }


    class ConnectionsComparator : DiffUtil.ItemCallback<Connection>(){
        override fun areItemsTheSame(oldItem: Connection, newItem: Connection): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Connection, newItem: Connection): Boolean {
            return oldItem == newItem
        }
    }
}