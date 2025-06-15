package com.maary.liveinpeace.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maary.liveinpeace.ui.theme.Typography
import com.maary.liveinpeace.R
import com.maary.liveinpeace.viewmodel.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.maary.liveinpeace.activity.HistoryActivity


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

        LaunchedEffect(notificationPermissionState) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(stringResource(R.string.app_name))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        //exit the settings screen
                        // this could be a popBackStack or finish depending on your navigation setup
                        // For example, if using Jetpack Navigation:
                         (context as? Activity)?.finish()
                        // If using a navigation component, you might want to use:
                        // navController.popBackStack()
                    }) {
                        Icon(imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.exit))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, HistoryActivity::class.java)
                        context.startActivity(intent)
                    } ) {
                        Icon(painter = painterResource(R.drawable.ic_action_history),
                            contentDescription = stringResource(R.string.connections_history))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ){

            /**
             * 1. 启用状态
             * 2. 提醒状态
             * 3. 音量保护
             * 3.1 安全音量阈值
             * 4. 隐藏桌面图标
             * */

            EnableForegroundRow(
                state = settingsViewModel.foregroundSwitchState.collectAsState().value,
                onCheckedChange = { settingsViewModel.foregroundSwitch() },
                onNotificationSettingsClicked = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
            )

            SwitchRow(
                title = stringResource(R.string.enable_watching),
                description = stringResource(R.string.enable_watching) /* todo */,
                state = settingsViewModel.alertSwitchState.collectAsState().value,
                onCheckedChange = { settingsViewModel.alertSwitch() }
            )

            SwitchRow(
                title = stringResource(R.string.protection),
                description = stringResource(R.string.protection) /* todo */,
                state = settingsViewModel.protectionSwitchState.collectAsState().value,
                onCheckedChange = { settingsViewModel.protectionSwitch() }
            )

            SwitchRow(
                title = stringResource(R.string.hide_in_launcher),
                description = stringResource(R.string.hide_in_launcher) /* todo */,
                state = settingsViewModel.hideInLauncherSwitchState.collectAsState().value,
                onCheckedChange = { settingsViewModel.hideInLauncherSwitch() }
            )
        }
    }
}