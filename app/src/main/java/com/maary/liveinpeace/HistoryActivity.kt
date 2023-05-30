package com.maary.liveinpeace

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.maary.liveinpeace.databinding.ActivityHistoryBinding
import java.time.LocalDate


class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val newConnectionActivityRequestCode = 1
    private val connectionViewModel: ConnectionViewModel by viewModels {
        ConnectionViewModelFactory((application as ConnectionsApplication).repository)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        setContentView(binding.root)

        val connectionAdapter = ConnectionListAdapter()
        binding.historyList.adapter = connectionAdapter
        binding.historyList.layoutManager = LinearLayoutManager(this)

        val today = LocalDate.now().toString()

        binding.toggleHistory.check(R.id.button_timeline)

        connectionViewModel.allConnectionsToday.observe(this) { connections ->
            connections.let { connectionAdapter.submitList(it) }
        }

        binding.toggleHistory.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.button_timeline) {
                connectionViewModel.allConnectionsToday.observe(this) { connections ->
                    connections.let { connectionAdapter.submitList(it) }
                }
            }
            if (checkedId == R.id.button_summary) {
                connectionViewModel.summaryToday.observe(this) { connections ->
                    connections.let { connectionAdapter.submitList(it) }
                }
            }
        }


    }
}