package com.maary.liveinpeace

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maary.liveinpeace.database.Connection
import java.util.concurrent.TimeUnit

class ConnectionListAdapter : ListAdapter<Connection, ConnectionListAdapter.ConnectionViewHolder>(ConnectionsComparator()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        return ConnectionViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind( current.name, current.type, current.duration)
    }

    class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val connectionIconView: ImageView = itemView.findViewById(R.id.device_icon)
        private val connectionDeviceNameView: TextView = itemView.findViewById(R.id.device_name)
        private val connectionDurationView: TextView = itemView.findViewById(R.id.device_connection_time)

        fun bind(deviceName: String?, type: Int?,duration: Long?) {
            connectionIconView.setImageResource(R.drawable.ic_headphone)
            connectionDeviceNameView.text = deviceName
            connectionDurationView.text = duration?.let { formatMilliseconds(itemView.context, it) }
        }

        companion object {
            fun create(parent: ViewGroup): ConnectionViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_connection, parent, false)
                return ConnectionViewHolder(view)
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

    class ConnectionsComparator : DiffUtil.ItemCallback<Connection>(){
        override fun areItemsTheSame(oldItem: Connection, newItem: Connection): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Connection, newItem: Connection): Boolean {
            return oldItem == newItem
        }
    }
}