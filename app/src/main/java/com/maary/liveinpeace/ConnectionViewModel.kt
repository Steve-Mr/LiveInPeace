package com.maary.liveinpeace

import androidx.lifecycle.*
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.database.ConnectionRepository
import java.sql.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ConnectionViewModel(private val connectionRepository: ConnectionRepository) : ViewModel() {

//    val allConnectionsOnDate: LiveData<List<Connection>> = connectionRepository.getAllConnectionsOnDate()
    val allConnectionsToday: LiveData<List<Connection>> = connectionRepository.allConnectionsToday.asLiveData()

    fun getAllConnectionsOnDate(queryDate: String): LiveData<List<Connection>> {
        return connectionRepository.getAllConnectionsOnDate(queryDate).asLiveData()
    }

    fun insert(connection: Connection) = viewModelScope.launch(Dispatchers.IO) {
        connectionRepository.insert(connection)
    }
}

class ConnectionViewModelFactory(private val connectionRepository: ConnectionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConnectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConnectionViewModel(connectionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel classes")
    }
}