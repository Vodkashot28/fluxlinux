package com.ivarna.fluxlinux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.ivarna.fluxlinux.ui.components.BottomTab
import com.ivarna.fluxlinux.ui.components.GlassBottomNavigation
import com.ivarna.fluxlinux.ui.components.GlassScaffold
import com.ivarna.fluxlinux.ui.theme.FluxLinuxTheme
import com.ivarna.fluxlinux.core.utils.StateManager
import com.ivarna.fluxlinux.core.utils.ThemePreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

// Screen navigation enum
enum class Screen {
    ONBOARDING,
    PREREQUISITES,
    HOME,
    SETTINGS,
    TROUBLESHOOTING,
    ROOT_ACCESS,
    INSTALL_WIZARD,
    DISTRO_SETTINGS
}

class MainActivity : ComponentActivity() {
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleScriptCallback(intent)
    }

    private fun handleScriptCallback(intent: android.content.Intent) {
        android.util.Log.d("FluxLinux", "handleScriptCallback called with action: ${intent.action}, data: ${intent.data}")
        // Handle Deep Link: fluxlinux://callback?result=success&name=setup_termux
        if (intent.action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "fluxlinux") {
            val uri = intent.data
            val result = uri?.getQueryParameter("result")
            val scriptName = uri?.getQueryParameter("name") ?: "unknown"
            val installedComponents = uri?.getQueryParameter("components")
            
            android.util.Log.d("FluxLinux", "Deep Link received: result=$result, scriptName=$scriptName")
            
            if (result == "success") {
                 // Check if this is a distro installation callback
                 if (scriptName.startsWith("distro_install_")) {
                     val distroId = scriptName.removePrefix("distro_install_")
                     StateManager.setDistroInstalled(this, distroId, true)
                     
                     // Mark components as installed if provided
                     installedComponents?.split(",")?.forEach { compId ->
                         if (compId.isNotBlank()) {
                             StateManager.setComponentInstalled(this, distroId, compId, true)
                         }
                     }
                     
                     android.widget.Toast.makeText(this, "$distroId Installed! ✅", android.widget.Toast.LENGTH_LONG).show()
                 } else if (scriptName.startsWith("distro_uninstall_")) {
                     val distroId = scriptName.removePrefix("distro_uninstall_")
                     StateManager.setDistroInstalled(this, distroId, false)
                     android.widget.Toast.makeText(this, "$distroId Uninstalled! 🗑️", android.widget.Toast.LENGTH_LONG).show()
                 } else {
                     StateManager.setScriptStatus(this, scriptName, true)
                     android.widget.Toast.makeText(this, "Script '$scriptName' completed! ✅", android.widget.Toast.LENGTH_LONG).show()
                 }
                 
                 // If setup_termux just finished, ensure X11 preferences are written
                 if (scriptName == "setup_termux") {
                     com.ivarna.fluxlinux.core.utils.TermuxX11Preferences.applyPreferences(this)
                 }
            } else {
                 // Handle failure
                 if (scriptName.startsWith("distro_install_")) {
                     val distroId = scriptName.removePrefix("distro_install_")
                     StateManager.setDistroInstalled(this, distroId, false) // Ensure it's marked as not installed
                 }
                 android.widget.Toast.makeText(this, "Script '$scriptName' failed! ❌", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        // Fallback for Extras (Legacy)
        else if (intent.hasExtra("script_result")) {
             val result = intent.getStringExtra("script_result")
             val scriptName = intent.getStringExtra("script_name") ?: "unknown"
             
             if (result == "success") {
                 StateManager.setScriptStatus(this, scriptName, true)
                 android.widget.Toast.makeText(this, "Script '$scriptName' completed successfully! ✅", android.widget.Toast.LENGTH_LONG).show()
             } else {
                 android.widget.Toast.makeText(this, "Script '$scriptName' failed! ❌", android.widget.Toast.LENGTH_LONG).show()
             }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class, ExperimentalHazeMaterialsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Watch Theme Preference
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferences(context) }
            
            // Lift state up
            var currentThemeMode by remember { mutableStateOf(themePrefs.getThemeMode()) }

            FluxLinuxTheme(themeMode = currentThemeMode) {
                val onboardingComplete = StateManager.isOnboardingComplete(this@MainActivity)
                
                // Permission State (Lifted for Settings and Home access)
                val permissionState = rememberPermissionState(
                    permission = "com.termux.permission.RUN_COMMAND"
                )

                // Navigation state
                var currentScreen by remember { 
                    mutableStateOf(if (onboardingComplete) Screen.HOME else Screen.ONBOARDING) 
                }
                
                var currentTab by remember { mutableStateOf(BottomTab.HOME) }
                
                // Selected Distro for Wizard/Settings
                var selectedDistro by remember { mutableStateOf<com.ivarna.fluxlinux.core.data.Distro?>(null) }
                
                // Refresh key to force UI update on resume
                var refreshKey by remember { mutableStateOf(0) }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            refreshKey++
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                
                // Helpers for service/activity
                val onStartServiceStub: (android.content.Intent) -> Unit = { intent ->
                    try { startService(intent) } catch (e: Exception) { android.util.Log.e("FluxLinux", "StartService failed", e) }
                }
                val onStartActivityStub: (android.content.Intent) -> Unit = { intent ->
                    try { startActivity(intent) } catch (e: Exception) { android.util.Log.e("FluxLinux", "StartActivity failed", e) }
                }
                
                // Navigation Callbacks
                val onNavigateToInstall: (com.ivarna.fluxlinux.core.data.Distro) -> Unit = { distro ->
                    selectedDistro = distro
                    currentScreen = Screen.INSTALL_WIZARD
                }
                val onNavigateToDistroSettings: (com.ivarna.fluxlinux.core.data.Distro) -> Unit = { distro ->
                    selectedDistro = distro
                    currentScreen = Screen.DISTRO_SETTINGS
                }
                
                @Composable
                fun MainScreenContent(
                    tab: BottomTab,
                    hazeState: HazeState
                ) {
                    when (tab) {
                        BottomTab.HOME -> {
                            com.ivarna.fluxlinux.ui.screens.HomeScreen(
                                permissionState = permissionState,
                                hazeState = hazeState,
                                scriptRefreshTrigger = refreshKey,
                                onStartService = onStartServiceStub,
                                onStartActivity = onStartActivityStub,
                                // Pass navigation callbacks
                                onNavigateToInstall = onNavigateToInstall,
                                onNavigateToSettings = onNavigateToDistroSettings
                            )
                        }
                        BottomTab.DISTROS -> {
                            com.ivarna.fluxlinux.ui.screens.DistroScreen(
                                permissionState = permissionState,
                                hazeState = hazeState,
                                onStartService = onStartServiceStub,
                                onStartActivity = onStartActivityStub,
                                onNavigateToInstall = onNavigateToInstall
                            )
                        }
                    }
                }

                // Helper for Top Bar
                @Composable
                fun TopBar(
                    hazeState: HazeState,
                    onSettingsClick: () -> Unit
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hazeChild(
                                state = hazeState,
                                style = HazeStyle(
                                    backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                    blurRadius = 20.dp,
                                    tint = null
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .windowInsetsPadding(WindowInsets.statusBars),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FluxLinux",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (StateManager.isTermuxInstalled(LocalContext.current)) {
                                   Text(
                                       text = StateManager.getPackageSize(LocalContext.current, "com.termux"),
                                       style = MaterialTheme.typography.labelSmall,
                                       color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                       modifier = Modifier.padding(end = 8.dp)
                                   )
                                }
                                
                                IconButton(onClick = onSettingsClick) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Show appropriate screen based on state
                when (currentScreen) {
                    Screen.ONBOARDING -> {
                        val showPrerequisites = remember { mutableStateOf(false) }
                        if (!showPrerequisites.value) {
                            com.ivarna.fluxlinux.ui.screens.OnboardingScreen(
                                onGetStarted = { showPrerequisites.value = true }
                            )
                        } else {
                            com.ivarna.fluxlinux.ui.screens.PrerequisitesScreen(
                                onComplete = {
                                    StateManager.setOnboardingComplete(this@MainActivity, true)
                                    currentScreen = Screen.HOME
                                }
                            )
                        }
                    }
                    Screen.HOME -> {
                        val hazeState = remember { HazeState() }
                        GlassScaffold(
                            hazeState = hazeState,
                            topBar = {
                                TopBar(
                                    hazeState = hazeState,
                                    onSettingsClick = { currentScreen = Screen.SETTINGS }
                                )
                            },
                            bottomBar = {
                                GlassBottomNavigation(
                                    selectedTab = currentTab,
                                    onTabSelected = { currentTab = it },
                                    hazeState = hazeState
                                )
                            }
                        ) {
                            MainScreenContent(
                                tab = currentTab,
                                hazeState = hazeState
                            )
                        }
                    }
                    Screen.SETTINGS -> {
                        com.ivarna.fluxlinux.ui.screens.SettingsScreen(
                            onBack = { currentScreen = Screen.HOME },
                            permissionState = permissionState,
                            onStartService = onStartServiceStub,
                            onStartActivity = onStartActivityStub,
                            onNavigateToOnboarding = {
                                StateManager.setOnboardingComplete(this@MainActivity, false)
                                currentScreen = Screen.ONBOARDING
                            },
                            onNavigateToTroubleshooting = { currentScreen = Screen.TROUBLESHOOTING },
                            onNavigateToRootCheck = { currentScreen = Screen.ROOT_ACCESS },
                            onThemeChanged = { newMode -> 
                                themePrefs.setThemeMode(newMode)
                                currentThemeMode = newMode 
                            },
                            currentTheme = currentThemeMode
                        )
                    }
                    Screen.TROUBLESHOOTING -> {
                        com.ivarna.fluxlinux.ui.screens.TroubleshootingScreen(
                            onBack = { currentScreen = Screen.SETTINGS }
                        )
                    }
                    Screen.PREREQUISITES -> { currentScreen = Screen.HOME }
                    Screen.ROOT_ACCESS -> {
                        com.ivarna.fluxlinux.ui.screens.RootAccessScreen(
                            onBack = { currentScreen = Screen.SETTINGS },
                            onEnableChroot = {
                                android.widget.Toast.makeText(this@MainActivity, "Chroot Mode Enabled", android.widget.Toast.LENGTH_SHORT).show()
                                currentScreen = Screen.SETTINGS
                            }
                        )
                    }
                    Screen.INSTALL_WIZARD -> {
                         val hazeState = remember { HazeState() }
                         if (selectedDistro != null) {
                             com.ivarna.fluxlinux.ui.screens.InstallConfigScreen(
                                 distro = selectedDistro!!,
                                 onBack = { currentScreen = Screen.HOME }, // Or Screen.DISTROS depending on where they came from? Let's just go Home for now or maintain history.
                                 // Actually for simplicity, back goes to tab view.
                                 hazeState = hazeState,
                                 onInstallStart = { components ->
                                     if (permissionState.status.isGranted) {
                                         val intent = com.ivarna.fluxlinux.core.data.TermuxIntentFactory.buildCompoundInstallIntent(this@MainActivity, selectedDistro!!, components)
                                         try {
                                             onStartServiceStub(intent)
                                             android.widget.Toast.makeText(this@MainActivity, "Starting Installation...", android.widget.Toast.LENGTH_LONG).show()
                                             currentScreen = Screen.HOME // Go back to home to see progress
                                         } catch (e: Exception) {
                                             android.util.Log.e("FluxLinux", "Install failed", e)
                                             android.widget.Toast.makeText(this@MainActivity, "Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                         }
                                     } else {
                                         permissionState.launchPermissionRequest()
                                     }
                                 }
                             )
                         } else {
                             currentScreen = Screen.HOME
                         }
                    }
                    Screen.DISTRO_SETTINGS -> {
                         val hazeState = remember { HazeState() }
                         if (selectedDistro != null) {
                             com.ivarna.fluxlinux.ui.screens.DistroSettingsScreen(
                                 distro = selectedDistro!!,
                                 onBack = { currentScreen = Screen.HOME },
                                 hazeState = hazeState,
                                 onInstallComponent = { component ->
                                     if (permissionState.status.isGranted) {
                                         val scriptManager = com.ivarna.fluxlinux.core.data.ScriptManager(this@MainActivity)
                                         val scriptContent = scriptManager.getScriptContent(component.scriptName)
                                         val intent = com.ivarna.fluxlinux.core.data.TermuxIntentFactory.buildRunFeatureScriptIntent(selectedDistro!!.id, scriptContent)
                                         try {
                                             onStartServiceStub(intent)
                                             android.widget.Toast.makeText(this@MainActivity, "Installing ${component.name}...", android.widget.Toast.LENGTH_SHORT).show()
                                         } catch(e: Exception) {
                                             android.util.Log.e("FluxLinux", "Component install failed", e)
                                         }
                                     } else {
                                         permissionState.launchPermissionRequest()
                                     }
                                 },
                                 onUninstallDistro = {
                                     // Navigate to Home first, then trigger uninstall
                                     // This is a bit disjointed. The uninstall logic was embedded in HomeScreen.
                                     // Using TermuxIntentFactory directly here.
                                      if (permissionState.status.isGranted) {
                                          val intent = com.ivarna.fluxlinux.core.data.TermuxIntentFactory.buildUninstallIntent(selectedDistro!!.id)
                                          try {
                                              onStartServiceStub(intent)
                                              StateManager.setDistroInstalled(this@MainActivity, selectedDistro!!.id, false)
                                              android.widget.Toast.makeText(this@MainActivity, "Uninstalling...", android.widget.Toast.LENGTH_SHORT).show()
                                              currentScreen = Screen.HOME
                                          } catch(e: Exception) {
                                              android.util.Log.e("FluxLinux", "Uninstall failed", e)
                                          }
                                      } else {
                                          permissionState.launchPermissionRequest()
                                      }
                                 },
                                 onStartActivity = onStartActivityStub,
                                 onNavigateToStart = { /* Not used in Settings, but if needed */ }
                             )
                         } else {
                             currentScreen = Screen.HOME
                         }
                    }
                }
            }
        }
    }
}

