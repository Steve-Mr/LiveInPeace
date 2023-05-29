package com.maary.liveinpeace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        setContentView(binding.root)

        val connectionAdapter = ConnectionListAdapter()
        binding.historyList.adapter = connectionAdapter
        binding.historyList.layoutManager = LinearLayoutManager(this)

        val today = LocalDate.now().toString()

        connectionViewModel.allConnectionsToday.observe(this) { connections ->
            connections.let { connectionAdapter.submitList(it) }
        }


    }
}