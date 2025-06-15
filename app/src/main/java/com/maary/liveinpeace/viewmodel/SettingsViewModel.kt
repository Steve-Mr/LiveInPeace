package com.maary.liveinpeace.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maary.liveinpeace.database.PreferenceRepository
import com.maary.liveinpeace.service.ForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val preferenceRepository: PreferenceRepository
): ViewModel() {

    private val _foregroundSwitchState = MutableStateFlow(false)
    val foregroundSwitchState : StateFlow<Boolean> = _foregroundSwitchState.asStateFlow()

    fun foregroundSwitch() {
        if(!_foregroundSwitchState.value) {
            startForegroundService()
        } else {
            stopForegroundService()
        }
    }

    private fun startForegroundService() {
        viewModelScope.launch {
            val intent = Intent(application, ForegroundService::class.java)
            application.startForegroundService(intent)
        }
    }

    private fun stopForegroundService() {
        viewModelScope.launch {
            val intent = Intent(application, ForegroundService::class.java)
            application.stopService(intent)
        }
    }

    private val _alertSwitchState = MutableStateFlow(false)
    val alertSwitchState : StateFlow<Boolean> = _alertSwitchState.asStateFlow()

    fun alertSwitch() {
        viewModelScope.launch {
            preferenceRepository.setWatchingState(!_alertSwitchState.value)
        }
    }

    private val _protectionSwitchState = MutableStateFlow(false)
    val protectionSwitchState : StateFlow<Boolean> = _protectionSwitchState.asStateFlow()

    fun protectionSwitch() {
        viewModelScope.launch {
            preferenceRepository.setEarProtection(!_protectionSwitchState.value)
        }
    }

    private val _hideInLauncherSwitchState = MutableStateFlow(false)
    val hideInLauncherSwitchState : StateFlow<Boolean> = _hideInLauncherSwitchState.asStateFlow()

    fun hideInLauncherSwitch() {
        viewModelScope.launch {
            val packageManager = application.packageManager
            val componentName = ComponentName(application, "${application.packageName}.MainActivityAlias")

            val newState = if(!_hideInLauncherSwitchState.value) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }

            packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
            _hideInLauncherSwitchState.value = !_hideInLauncherSwitchState.value
            preferenceRepository.setHideInLauncher(!_hideInLauncherSwitchState.value)
        }
    }

    init {
        preferenceRepository.isServiceRunning().onEach {
            _foregroundSwitchState.value = it
        }.launchIn(viewModelScope)
        preferenceRepository.getWatchingState().onEach {
            _alertSwitchState.value = it
        }.launchIn(viewModelScope)
        preferenceRepository.isEarProtectionOn().onEach {
            _protectionSwitchState.value = it
        }.launchIn(viewModelScope)
        preferenceRepository.isHideInLauncher().onEach {
            _hideInLauncherSwitchState.value = it
        }.launchIn(viewModelScope)
    }

}