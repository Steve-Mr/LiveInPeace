package com.maary.liveinpeace.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.maary.liveinpeace.ConnectionListAdapter
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_CONNECTIONS_UPDATE
import com.maary.liveinpeace.Constants.Companion.EXTRA_CONNECTIONS_LIST
import com.maary.liveinpeace.Constants.Companion.PATTERN_DATE_BUTTON
import com.maary.liveinpeace.Constants.Companion.PATTERN_DATE_DATABASE
import com.maary.liveinpeace.LiveInPeaceApplication
import com.maary.liveinpeace.R
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.databinding.ActivityHistoryBinding
import com.maary.liveinpeace.viewmodel.ConnectionViewModel
import com.maary.liveinpeace.viewmodel.ConnectionViewModelFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale


// Remove DeviceMapChangeListener from the class declaration
class HistoryActivity : AppCompatActivity() {

    //todo add swipt to change date
    //todo show current connections in a different color
    //todo show connections start time and end time in the list
    private lateinit var binding: ActivityHistoryBinding
    private val connectionViewModel: ConnectionViewModel by viewModels {
        ConnectionViewModelFactory((application as LiveInPeaceApplication).repository)
    }
    // Adapter for currently connected devices
    private val currentAdapter = ConnectionListAdapter()
    // Adapter for historical connections (from ViewModel)
    private val historyAdapter = ConnectionListAdapter() // Use a separate adapter instance

    // Declare the BroadcastReceiver
    private var connectionsUpdateReceiver: BroadcastReceiver? = null

    override fun onResume() {
        super.onResume()
        // Register the receiver
        registerConnectionsUpdateReceiver()
    }

    override fun onPause() {
        super.onPause()
        // Unregister the receiver
        unregisterReceiver(connectionsUpdateReceiver)
        connectionsUpdateReceiver = null // Allow garbage collection
    }

    // Calculates duration for currently connected items based on their connect time
    private fun calculateCurrentConnectionsDuration(currentList: List<Connection>): List<Connection> {
        val now = System.currentTimeMillis()
        return currentList.map { connection ->
            if (connection.connectedTime != null && connection.disconnectedTime == null) {
                val duration = now - connection.connectedTime
                // Create a new Connection object with updated duration
                // Ensure other fields are copied correctly. Using copy() is ideal.
                connection.copy(duration = duration) // Use copy for data classes
            } else {
                // If already disconnected or no connectedTime, return as is
                connection
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use DateTimeFormatter for LocalDate
        // Define the formatter using the pattern from Constants
        val dbDateFormatter = DateTimeFormatter.ofPattern(PATTERN_DATE_DATABASE, Locale.getDefault())
        var pickedDate: String = LocalDate.now().format(dbDateFormatter) // Use the correct formatter

        // Makes only dates from today backward selectable.
        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())

        var datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setCalendarConstraints(constraintsBuilder.build())
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        // Function to update the historical list based on ViewModel
        fun updateHistoryList(checkedId: Int) {
            val listToObserve = if (checkedId == R.id.button_timeline) {
                connectionViewModel.getAllConnectionsOnDate(pickedDate)
            } else { // R.id.button_summary
                connectionViewModel.getSummaryOnDate(pickedDate)
            }
            listToObserve.observe(this) { connections ->
                connections?.let { historyAdapter.submitList(it) }
            }
            // Note: updateCurrentAdapter() is now called by the broadcast receiver,
            // so it's removed from here unless you need to clear it on date change.
        }

        fun changeDate(dateInMilli: Long?) {
            val effectiveDateInMillis = dateInMilli ?: System.currentTimeMillis() // Use current time if null
            binding.buttonCalendar.text = formatMillisecondsToDate(effectiveDateInMillis, PATTERN_DATE_BUTTON)
            pickedDate = formatMillisecondsToDate(effectiveDateInMillis, PATTERN_DATE_DATABASE)
            updateHistoryList(binding.toggleHistory.checkedButtonId)
            // Optionally clear the current list when date changes, or let the broadcast handle it
            // updateCurrentConnectionsView(emptyList()) // Example: Clear current list
        }

        // Setup historical RecyclerView
        binding.historyList.isNestedScrollingEnabled = false
        binding.historyList.adapter = historyAdapter
        binding.historyList.layoutManager = LinearLayoutManager(this)

        binding.toggleHistory.check(R.id.button_timeline)

        binding.activityHistoryToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.buttonCalendar.text = formatMillisecondsToDate(System.currentTimeMillis(), PATTERN_DATE_BUTTON)

        binding.buttonCalendar.setOnClickListener {
            datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
        }

        binding.buttonCalendar.setOnLongClickListener {
            changeDate(System.currentTimeMillis())
            // Rebuild date picker if needed, or just reset selection
            datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setCalendarConstraints(constraintsBuilder.build())
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            true
        }

        // Setup current connections RecyclerView
        binding.currentList.isNestedScrollingEnabled = false
        binding.currentList.adapter = currentAdapter
        binding.currentList.layoutManager = LinearLayoutManager(this)

        // Initial state for the current list view
        updateCurrentConnectionsView(emptyList()) // Start with empty, wait for broadcast

        // Initial load for history list
        updateHistoryList(binding.toggleHistory.checkedButtonId)

        // Listener for history view toggle (Timeline vs Summary)
        binding.toggleHistory.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) { // Only react when a button becomes checked
                updateHistoryList(checkedId)
            }
        }

        // Listener for Date Picker confirmation
        datePicker.addOnPositiveButtonClickListener { selection ->
            changeDate(selection) // selection should be Long?
        }
    }

    // Renamed and modified function to update the 'current' list RecyclerView
    private fun updateCurrentConnectionsView(connections: List<Connection>) {
        val processedList = calculateCurrentConnectionsDuration(connections)
        currentAdapter.submitList(processedList)
        // Control visibility of the "Currently Connected" title
        binding.titleCurrent.visibility = if (processedList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun formatMillisecondsToDate(milliseconds: Long?, pattern: String): String {
        // Default to now if milliseconds is null
        val millis = milliseconds ?: System.currentTimeMillis()
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return dateFormat.format(calendar.time)
    }

    // --- BroadcastReceiver Implementation ---

    private fun registerConnectionsUpdateReceiver() {
        if (connectionsUpdateReceiver == null) {
            connectionsUpdateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == BROADCAST_ACTION_CONNECTIONS_UPDATE) {
                        val connectionsList: ArrayList<Connection>? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableArrayListExtra(EXTRA_CONNECTIONS_LIST, Connection::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableArrayListExtra(EXTRA_CONNECTIONS_LIST)
                            }

                        Log.d("HistoryActivity", "Received connection update: ${connectionsList?.size ?: 0} items")
                        // Update the UI with the received list, handle null case
                        updateCurrentConnectionsView(connectionsList ?: emptyList())
                    }
                }
            }
            val filter = IntentFilter(BROADCAST_ACTION_CONNECTIONS_UPDATE)
            // Use ContextCompat for compatibility and specifying receiver export behavior
            ContextCompat.registerReceiver(this, connectionsUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            Log.d("HistoryActivity", "ConnectionsUpdateReceiver registered")
        }
    }
}