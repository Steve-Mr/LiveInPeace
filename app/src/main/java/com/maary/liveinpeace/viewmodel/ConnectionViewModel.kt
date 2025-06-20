package com.maary.liveinpeace.viewmodel

import androidx.lifecycle.*
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.database.ConnectionRepository

class ConnectionViewModel(private val connectionRepository: ConnectionRepository) : ViewModel() {

    fun getAllConnectionsOnDate(queryDate: String): LiveData<List<Connection>> {
        return connectionRepository.getAllConnectionsOnDate(queryDate).asLiveData()
    }

    fun getSummaryOnDate(queryDate: String): LiveData<List<Connection>> {
        return connectionRepository.getSummaryOnDate(queryDate).asLiveData()
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