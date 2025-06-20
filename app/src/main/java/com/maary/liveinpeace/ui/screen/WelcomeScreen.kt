package com.maary.liveinpeace.ui.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maary.liveinpeace.R
import com.maary.liveinpeace.activity.MainActivity
import com.maary.liveinpeace.viewmodel.WelcomeViewModel

@SuppressLint("BatteryLife")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(welcomeViewModel: WelcomeViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val hasNotificationPermission by welcomeViewModel.hasNotificationPermission.collectAsState()
    val isIgnoringBatteryOptimizations by welcomeViewModel.isIgnoringBatteryOptimizations.collectAsState()
    val showIconState by welcomeViewModel.showIconState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            welcomeViewModel.onPermissionResult(isGranted)
        }
    )

    LaunchedEffect(Unit) {
        val alreadyGranted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        welcomeViewModel.onPermissionResult(alreadyGranted)
        welcomeViewModel.checkBatteryOptimizationStatus()
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if ( event == Lifecycle.Event.ON_RESUME) {
                welcomeViewModel.checkBatteryOptimizationStatus()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary),
                title = {
                    Text(stringResource(R.string.welcome))
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.exit)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.inversePrimary)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp + innerPadding.calculateTopPadding()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = stringResource(R.string.nessery_permissions),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                SettingsItem(GroupPosition.SINGLE , containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    SwitchRow(
                        title = stringResource(R.string.notification_permission),
                        description = stringResource(R.string.notification_permission_description),
                        state = hasNotificationPermission,
                        switchColor = MaterialTheme.colorScheme.tertiary
                    ) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = stringResource(R.string.optional_permissions),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                SettingsItem(GroupPosition.TOP) {
                    SwitchRow(
                        title = stringResource(R.string.disable_battery_optimization),
                        description = stringResource(R.string.disable_battery_optimization_description),
                        state = isIgnoringBatteryOptimizations,
                    ) {
                        val batteryIntent =
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        batteryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .data = "package:${context.packageName}".toUri()
                        context.startActivity(batteryIntent)
                    }
                }
                SettingsItem(GroupPosition.BOTTOM) {
                    SwitchRow(
                        title = stringResource(R.string.show_icon),
                        description = stringResource(R.string.show_icon_description),
                        state = showIconState,
                    ) {
                        welcomeViewModel.toggleShowIcon()
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End) {
                Button(
                    modifier = Modifier.padding(8.dp),
                    enabled = hasNotificationPermission,
                    onClick = {
                        welcomeViewModel.welcomeFinished()
//                        (context as? Activity)?.finish()
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }) {
                    Text(stringResource(R.string.finish))
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

