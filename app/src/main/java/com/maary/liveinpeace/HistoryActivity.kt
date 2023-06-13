package com.maary.liveinpeace

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.maary.liveinpeace.Constants.Companion.PATTERN_DATE_BUTTON
import com.maary.liveinpeace.Constants.Companion.PATTERN_DATE_DATABASE
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.databinding.ActivityHistoryBinding
import com.maary.liveinpeace.service.ForegroundService
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale


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

        var pickedDate : String = LocalDate.now().toString()

        val connectionAdapter = ConnectionListAdapter()

        // Makes only dates from today forward selectable.
        val constraintsBuilder =
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())

        var datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setCalendarConstraints(constraintsBuilder.build())
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        fun updateHistoryList(checkedId: Int){
            if (checkedId == R.id.button_timeline) {
                connectionViewModel.getAllConnectionsOnDate(pickedDate).observe(this) { connections ->
                    connections.let { connectionAdapter.submitList(it) }
                }
            }
            if (checkedId == R.id.button_summary) {
                connectionViewModel.getSummaryOnDate(pickedDate).observe(this) { connections ->
                    connections.let { connectionAdapter.submitList(it) }
                }
            }
            updateCurrentAdapter()
        }

        fun changeDate(dateInMilli: Long?){
            if (dateInMilli == null) return changeDate(System.currentTimeMillis())
            binding.buttonCalendar.text = formatMillisecondsToDate(dateInMilli, PATTERN_DATE_BUTTON)
            pickedDate = formatMillisecondsToDate(dateInMilli, PATTERN_DATE_DATABASE)
            updateHistoryList(binding.toggleHistory.checkedButtonId)
            updateCurrentAdapter()
        }

        binding.historyList.isNestedScrollingEnabled = false
        binding.historyList.adapter = connectionAdapter
        binding.historyList.layoutManager = LinearLayoutManager(this)

        binding.toggleHistory.check(R.id.button_timeline)

        binding.activityHistoryToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.buttonCalendar.text = formatMillisecondsToDate(System.currentTimeMillis(), PATTERN_DATE_BUTTON)

        binding.buttonCalendar.setOnClickListener {
            datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
        }

        binding.buttonCalendar.setOnLongClickListener{
            changeDate(System.currentTimeMillis())
            datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setCalendarConstraints(constraintsBuilder.build())
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            true
        }

        binding.currentList.isNestedScrollingEnabled = false
        binding.currentList.adapter = currentAdapter
        binding.currentList.layoutManager = LinearLayoutManager(this)

        updateCurrentAdapter()

        connectionViewModel.getAllConnectionsOnDate(pickedDate).observe(this) { connections ->
            connections.let { connectionAdapter.submitList(it) }
        }

        binding.toggleHistory.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            updateHistoryList(checkedId)
        }

        datePicker.addOnPositiveButtonClickListener {
            changeDate(datePicker.selection)
        }
    }

    private fun updateCurrentAdapter(){
        currentAdapter.submitList(currentConnectionsDuration(ForegroundService.getConnections()))
        if (currentAdapter.itemCount == 0){
            binding.titleCurrent.visibility = View.GONE
        }else{
            binding.titleCurrent.visibility = View.VISIBLE
        }
    }

    override fun onDeviceMapChanged(deviceMap: Map<String, Connection>) {
        if (deviceMap.isEmpty()){
            binding.titleCurrent.visibility = View.GONE
        }else{
            binding.titleCurrent.visibility = View.VISIBLE
        }
        currentAdapter.submitList(currentConnectionsDuration(deviceMap.values.toMutableList()))
    }

    private fun formatMillisecondsToDate(milliseconds: Long?, pattern: String): String {
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (milliseconds != null) {
            calendar.timeInMillis = milliseconds
        }
        return dateFormat.format(calendar.time)
    }
}