package com.maary.liveinpeace

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.databinding.ActivityHistoryBinding
import com.maary.liveinpeace.service.ForegroundService
import java.time.LocalDate


class HistoryActivity : AppCompatActivity(), DeviceMapChangeListener {

    private lateinit var binding: ActivityHistoryBinding
    private val connectionViewModel: ConnectionViewModel by viewModels {
        ConnectionViewModelFactory((application as ConnectionsApplication).repository)
    }
    private val currentAdapter = ConnectionListAdapter()

    override fun onResume() {
        super.onResume()
        ForegroundService.addDeviceMapChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        ForegroundService.removeDeviceMapChangeListener(this)
    }

    private fun currentConnectionsDuration(currentList: MutableList<Connection>) : MutableList<Connection>{
        val now = System.currentTimeMillis()

        for ( (index, connection) in currentList.withIndex()){
            val connectedTime = connection.connectedTime
            val duration = now - connectedTime!!
            currentList[index] = Connection(
                name = connection.name,
                type = connection.type,
                connectedTime = connection.connectedTime,
                disconnectedTime = null,
                duration = duration,
                date = connection.date
            )
        }

        return currentList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        setContentView(binding.root)

        val connectionAdapter = ConnectionListAdapter()
        binding.historyList.adapter = connectionAdapter
        binding.historyList.layoutManager = LinearLayoutManager(this)

        binding.toggleHistory.check(R.id.button_timeline)

        binding.activityHistoryToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.currentList.adapter = currentAdapter
        binding.currentList.layoutManager = LinearLayoutManager(this)

        currentAdapter.submitList(currentConnectionsDuration(ForegroundService.getConnections()))

        connectionViewModel.getAllConnectionsOnDate(LocalDate.now().toString()).observe(this) { connections ->
            connections.let { connectionAdapter.submitList(it) }
        }

        binding.toggleHistory.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.button_timeline) {
                connectionViewModel.getAllConnectionsOnDate(LocalDate.now().toString()).observe(this) { connections ->
                    connections.let { connectionAdapter.submitList(it) }
                }
            }
            if (checkedId == R.id.button_summary) {
                connectionViewModel.getSummaryOnDate(LocalDate.now().toString()).observe(this) { connections ->
                    connections.let { connectionAdapter.submitList(it) }

                }
            }
        }


    }

    override fun onDeviceMapChanged(deviceMap: Map<String, Connection>) {
        currentAdapter.submitList(currentConnectionsDuration(ForegroundService.getConnections()))
    }
}