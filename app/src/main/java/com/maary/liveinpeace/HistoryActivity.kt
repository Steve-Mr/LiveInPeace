package com.maary.liveinpeace

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.maary.liveinpeace.databinding.ActivityHistoryBinding
import java.time.LocalDate


class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
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

        binding.toggleHistory.check(R.id.button_timeline)

        binding.activityHistoryToolbar.setNavigationOnClickListener {
            finish()
        }

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
}