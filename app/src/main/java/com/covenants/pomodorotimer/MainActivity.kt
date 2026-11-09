package com.covenants.pomodorotimer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.covenants.pomodorotimer.core.model.FocusTask
import com.covenants.pomodorotimer.data.TimerEvent
import com.covenants.pomodorotimer.platform.TimerNotifier
import com.covenants.pomodorotimer.ui.AppUiState
import com.covenants.pomodorotimer.ui.MainViewModel
import com.covenants.pomodorotimer.ui.components.BottomNavBar
import com.covenants.pomodorotimer.ui.components.OrbitBackground
import com.covenants.pomodorotimer.ui.screens.ActiveScreen
import com.covenants.pomodorotimer.ui.screens.AddTaskDialog
import com.covenants.pomodorotimer.ui.screens.ConstellationsScreen
import com.covenants.pomodorotimer.ui.screens.HomeScreen
import com.covenants.pomodorotimer.ui.screens.InsightsScreen
import com.covenants.pomodorotimer.ui.screens.PlusScreen
import com.covenants.pomodorotimer.ui.screens.RoutineScreen
import com.covenants.pomodorotimer.ui.screens.SettingsScreen
import com.covenants.pomodorotimer.ui.screens.TasksScreen
import com.covenants.pomodorotimer.ui.theme.PomodoroTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            container = (application as PomodoroApp).container,
            context = applicationContext,
        )
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) viewModel.exportCsv(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAlarmLaunch(intent)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            PomodoroTheme(settings = state.settings) {
                OrbitApp(
                    viewModel = viewModel,
                    state = state,
                    tasks = tasks,
                    hasNotificationPermission = ::hasNotificationPermission,
                    canScheduleExactAlarms = ::canScheduleExactAlarms,
                    canUseFullScreenIntent = ::canUseFullScreenIntent,
                    onRequestNotifications = ::requestNotificationPermission,
                    onRequestExactAlarm = ::openExactAlarmSettings,
                    onRequestFullScreen = ::openFullScreenIntentSettings,
                    onExport = ::exportHistory,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlarmLaunch(intent)
    }

    private fun handleAlarmLaunch(intent: Intent?) {
        if (intent?.getBooleanExtra(TimerNotifier.EXTRA_ALARM_LAUNCH, false) != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        intent.removeExtra(TimerNotifier.EXTRA_ALARM_LAUNCH)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName"),
                )
            )
        }.onFailure {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.canUseFullScreenIntent()
    }

    private fun openFullScreenIntentSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName"),
                    )
                )
            }.onFailure {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    private fun exportHistory() {
        exportLauncher.launch("orbit-sessions-${LocalDate.now()}.csv")
    }
}

@Composable
fun OrbitApp(
    viewModel: MainViewModel,
    state: AppUiState,
    tasks: List<FocusTask>,
    hasNotificationPermission: () -> Boolean,
    canScheduleExactAlarms: () -> Boolean,
    canUseFullScreenIntent: () -> Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onExport: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddTask by rememberSaveable { mutableStateOf(false) }

    var permissionTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsGranted = remember(permissionTick) { hasNotificationPermission() }
    val exactAlarmsAllowed = remember(permissionTick) { canScheduleExactAlarms() }
    val fullScreenAllowed = remember(permissionTick) { canUseFullScreenIntent() }

    // Returning from Android settings can grant exact-alarm or full-screen access. Re-run the
    // repository reconciliation so a running timer moves from an inexact to an exact alarm.
    LaunchedEffect(permissionTick) {
        if (permissionTick > 0) viewModel.reconcile()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: "home"
    val showBottomBar = route == "home" || route == "tasks" || route == "insights" || route == "settings"
    val selectedBottom = when (route) {
        "settings" -> "you"
        else -> route
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TimerEvent.StageCompleted -> {
                    val message = when {
                        event.stage == com.covenants.pomodorotimer.core.model.FocusStage.WORK ->
                            if (event.nextStage == com.covenants.pomodorotimer.core.model.FocusStage.LONG_BREAK) {
                                "Focus complete. Long break ready."
                            } else {
                                "Focus complete. Short break ready."
                            }
                        else -> "Break complete. Ready to focus."
                    }
                    snackbarHostState.showSnackbar(message)
                }
                TimerEvent.CountdownCompleted -> snackbarHostState.showSnackbar("Countdown finished.")
                TimerEvent.Stopped -> snackbarHostState.showSnackbar("Session recorded as stopped.")
                is TimerEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is TimerEvent.Started -> Unit
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // A session restored from disk (or started anywhere) opens the live screen.
    LaunchedEffect(state.active?.sessionId, state.active?.status) {
        val active = state.active
        if (active != null &&
            (active.status == com.covenants.pomodorotimer.core.model.TimerStatus.RUNNING ||
                active.status == com.covenants.pomodorotimer.core.model.TimerStatus.PAUSED ||
                (active.status == com.covenants.pomodorotimer.core.model.TimerStatus.IDLE && active.stage != null)) &&
            route != "active"
        ) {
            kotlinx.coroutines.delay(200L)
            navController.navigate("active") { launchSingleTop = true }
        }
    }

    val startWithPermission: (() -> Unit) -> Unit = { action ->
        if (!notificationsGranted) onRequestNotifications()
        action()
    }

    OrbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(
                        selected = selectedBottom,
                        onSelect = { target ->
                            val destination = if (target == "you") "settings" else target
                            navController.navigate(destination) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable("home") {
                        HomeScreen(
                            state = state,
                            tasks = tasks,
                            onSelectMode = viewModel::selectMode,
                            onStartFocus = { task ->
                                startWithPermission { viewModel.startFocus(task) }
                            },
                            onStartCountdown = { duration ->
                                startWithPermission { viewModel.startCountdownMs(duration) }
                            },
                            onStartStopwatch = {
                                startWithPermission { viewModel.startStopwatch() }
                            },
                            onOpenActive = { navController.navigate("active") { launchSingleTop = true } },
                            onSaveRoutine = viewModel::saveRoutine,
                            onNavigateRoutine = { navController.navigate("routine") },
                            onNavigateConstellations = { navController.navigate("constellations") },
                            onAddTask = { showAddTask = true },
                            onOpenSettings = { navController.navigate("settings") },
                        )
                    }

                    composable("active") {
                        ActiveScreen(
                            state = state,
                            notificationsGranted = notificationsGranted,
                            exactAlarmsAllowed = exactAlarmsAllowed,
                            onBack = { navController.popBackStack() },
                            onTogglePause = viewModel::togglePause,
                            onAddFive = viewModel::addFiveMinutes,
                            onSkip = viewModel::skip,
                            onStop = {
                                viewModel.stop()
                                navController.popBackStack("home", inclusive = false)
                            },
                            onStartBreakNow = viewModel::startBreakNow,
                            onDismissBreak = {
                                viewModel.dismissWaitingBreak()
                                navController.popBackStack("home", inclusive = false)
                            },
                            onAcknowledge = {
                                viewModel.acknowledgeCompleted()
                                navController.popBackStack("home", inclusive = false)
                            },
                            onRequestNotifications = onRequestNotifications,
                            onRequestExactAlarm = onRequestExactAlarm,
                        )
                    }

                    composable("tasks") {
                        TasksScreen(
                            state = state,
                            tasks = tasks,
                            onToggleTask = viewModel::toggleTask,
                            onDeleteTask = viewModel::deleteTask,
                            onAddTask = viewModel::addTask,
                            onStartFocus = { task ->
                                startWithPermission { viewModel.startFocus(task) }
                            },
                            onOpenActive = { navController.navigate("active") { launchSingleTop = true } },
                        )
                    }

                    composable("insights") {
                        InsightsScreen(state = state)
                    }

                    composable("settings") {
                        SettingsScreen(
                            settings = state.settings,
                            notificationsGranted = notificationsGranted,
                            exactAlarmsAllowed = exactAlarmsAllowed,
                            onSetTheme = viewModel::setTheme,
                            onSetReduceMotion = viewModel::setReduceMotion,
                            onSetHighContrast = viewModel::setHighContrast,
                            onSetNotifications = viewModel::setNotifications,
                            onSetSound = viewModel::setSound,
                            onSetChime = viewModel::setChime,
                            onSetHaptics = viewModel::setHaptics,
                            onSetFullScreenAlerts = viewModel::setFullScreenAlerts,
                            fullScreenAlertsAllowed = fullScreenAllowed,
                            onRequestFullScreen = onRequestFullScreen,
                            onRequestNotifications = onRequestNotifications,
                            onRequestExactAlarm = onRequestExactAlarm,
                            onExport = onExport,
                            onDeleteHistory = viewModel::deleteAllHistory,
                            onNavigateRoutine = { navController.navigate("routine") },
                            onNavigateConstellations = { navController.navigate("constellations") },
                            onNavigatePlus = { navController.navigate("plus") },
                        )
                    }

                    composable("constellations") {
                        ConstellationsScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onSeeHistory = {
                                navController.navigate("insights") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                        )
                    }

                    composable("routine") {
                        RoutineScreen(
                            routine = state.routine,
                            onBack = { navController.popBackStack() },
                            onSave = {
                                viewModel.saveRoutine(it)
                                navController.popBackStack()
                            },
                        )
                    }

                    composable("plus") {
                        PlusScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    if (showAddTask) {
        AddTaskDialog(
            onDismiss = { showAddTask = false },
            onConfirm = { title, tag, estimate ->
                viewModel.addTask(title, tag, estimate)
                showAddTask = false
            },
        )
    }
}
