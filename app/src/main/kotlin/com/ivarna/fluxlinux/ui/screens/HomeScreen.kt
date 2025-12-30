package com.ivarna.fluxlinux.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.fluxlinux.core.data.DistroRepository
import com.ivarna.fluxlinux.core.data.Distro
import com.ivarna.fluxlinux.core.data.ScriptManager
import com.ivarna.fluxlinux.core.data.TermuxIntentFactory
import com.ivarna.fluxlinux.core.utils.ApkInstaller
import com.ivarna.fluxlinux.core.utils.StateManager
import com.ivarna.fluxlinux.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun HomeScreen(
    permissionState: PermissionState,
    hazeState: HazeState,
    scriptRefreshTrigger: Int = 0,
    onStartService: (android.content.Intent) -> Unit,
    onStartActivity: (android.content.Intent) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State for Uninstall Dialog
    val distroToUninstall = remember { mutableStateOf<com.ivarna.fluxlinux.core.data.Distro?>(null) }
    // State for Manual Root Uninstall Instruction Dialog
    val manualUninstallDistro = remember { mutableStateOf<com.ivarna.fluxlinux.core.data.Distro?>(null) }
    
    // Refresh key to trigger recomposition
    val refreshKey = remember { mutableStateOf(0) }

    // React to external refresh trigger (from MainActivity)
    LaunchedEffect(scriptRefreshTrigger) {
        if (scriptRefreshTrigger > 0) {
            refreshKey.value++
        }
    }
    

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Trigger initial refresh on mount
        LaunchedEffect(Unit) {
            refreshKey.value++
        }
        
        // Installed Distros Detection
        val installedDistros = remember(refreshKey.value) {
            val installedIds = StateManager.getInstalledDistros(context)
            DistroRepository.supportedDistros.filter { installedIds.contains(it.id) }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Floating Keyboard Toggle
        var isFloatingKeyboardRunning by remember { mutableStateOf(false) }
        val hasOverlayPermission = remember { android.provider.Settings.canDrawOverlays(context) }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Floating Keyboard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⌨️ Keyboard Toggle Button",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasOverlayPermission) "Floating button to toggle keyboard" else "Overlay permission required",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                if (hasOverlayPermission) {
                    Button(
                        onClick = {
                            if (!isFloatingKeyboardRunning) {
                                // Start service
                                try {
                                    val intent = android.content.Intent(context, com.ivarna.fluxlinux.core.services.FloatingKeyboardService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                    isFloatingKeyboardRunning = true
                                    android.widget.Toast.makeText(context, "Floating keyboard enabled", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Stop service
                                try {
                                    val intent = android.content.Intent(context, com.ivarna.fluxlinux.core.services.FloatingKeyboardService::class.java)
                                    context.stopService(intent)
                                    isFloatingKeyboardRunning = false
                                    android.widget.Toast.makeText(context, "Floating keyboard disabled", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFloatingKeyboardRunning) Color(0xFFFF5252) else FluxAccentCyan
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (isFloatingKeyboardRunning) "Disable" else "Enable",
                            color = if (isFloatingKeyboardRunning) Color.White else Color.Black,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not open settings", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FluxAccentMagenta),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Permission", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Installed Distros Section
        Text(
            text = "Installed Distros",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show empty state or distro list
        if (installedDistros.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No distros installed yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Install a distribution from the Distros tab",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Distro list
            installedDistros.forEach { distro ->
                com.ivarna.fluxlinux.ui.components.DistroCard(
                    distro = distro,
                    isInstalled = true,
                    onInstall = {}, // Won't be clicked
                    onUninstall = {
                        if (permissionState.status.isGranted) {
                            distroToUninstall.value = distro
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    },
                    onLaunchCli = {
                        if (permissionState.status.isGranted) {
                            val intent = TermuxIntentFactory.buildLaunchCliIntent(distro.id)
                            try {
                                onStartService(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("FluxLinux", "Launch CLI failed", e)
                            }
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    },
                    onLaunchGui = {
                        if (permissionState.status.isGranted) {
                            // Start floating keyboard service if overlay permission is granted
                            if (android.provider.Settings.canDrawOverlays(context)) {
                                try {
                                    val keyboardIntent = android.content.Intent(context, com.ivarna.fluxlinux.core.services.FloatingKeyboardService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(keyboardIntent)
                                    } else {
                                        context.startService(keyboardIntent)
                                    }
                                    isFloatingKeyboardRunning = true
                                } catch (e: Exception) {
                                    android.util.Log.e("FluxLinux", "Failed to start keyboard service", e)
                                }
                            }
                            
                            // Launch GUI
                            val intent = TermuxIntentFactory.buildLaunchGuiIntent(distro.id)
                            try {
                                onStartService(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("FluxLinux", "Launch GUI failed", e)
                            }
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    },
                    onWebDevInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_webdev_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Web Dev Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    webDevDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        "Includes: VS Code, Node.js (v20), Python 3, Firefox, Chromium, Git"
                    } else null,
                    onAppDevInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_appdev_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting App Dev Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    appDevDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        "Includes: Android Studio (IntelliJ), Flutter, SDK/NDK, React Native, Kotlin"
                    } else null,
                    onGenDevInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_gengdev_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting General Dev Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    genDevDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) 
                        "Installs Rust, Go, C++, LunarVim, VS Code & more." else null,
                    onCustomize = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                         {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_customization_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Desktop Customization...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start customization", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                         }
                    } else null,
                    customizeDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "Applies Themes, Fonts, Wallpapers & 2x Scaling." else null,
                    onEnableHwAccel = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                         {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_hw_accel_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting GPU Acceleration Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start accelerator", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                         }
                    } else null,
                    hwAccelDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "Turnip (Adreno) or VirGL (All GPUs)" else null,
                    onGameDevInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_gamedev_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Game Dev Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    gameDevDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "Installs Godot, Ren'Py, LÖVE & Python Libs." else null,
                    onDataScienceInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_datascience_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Data Science Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    dataScienceDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "Python (AI/ML), R, Julia, Jupyter & IDEs." else null,
                    onCyberSecInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_cybersec_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting CyberSec Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    cyberSecDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "Nmap, Wireshark, Metasploit, Aircrack-ng..." else null,
                    onVideoEditInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_video_editing_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Video Edit Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    videoEditDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "Kdenlive, Shotcut, VLC, FFmpeg & more." else null,
                    onDesignInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_graphic_design_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Design Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    designDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "GIMP, Inkscape, Krita, Blender & more." else null,
                    onOfficeInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_office_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Office Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    officeDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "LibreOffice, Thunderbird & more." else null,
                    onEmulationInstall = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN) {
                        {
                            if (permissionState.status.isGranted) {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_emulation_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Emulation Setup...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start setup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        }
                    } else null,
                    emulationDescription = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN)
                        "Box64, xow64-wine, Heroic, RetroArch." else null
                )
            }
        }
    }


        
        Spacer(modifier = Modifier.height(100.dp))
    
    if (distroToUninstall.value != null) {
        val distro = distroToUninstall.value!!
        AlertDialog(
            onDismissRequest = { distroToUninstall.value = null },
            title = { Text("Uninstall ${distro.name}?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all data associated with this distribution. This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(
                    onClick = {
                        // Logic Split: Standard (Intent) vs Chroot (Manual Root Copy-Paste)
                        val isChroot = distro.id == "debian_chroot" || distro.id == "debian13_chroot" || distro.id == "arch_chroot" || distro.id.contains("chroot")
                        
                        if (isChroot) {
                            // Close the main confirmation dialog first
                            distroToUninstall.value = null
                            
                            // Show the Instruction Dialog
                            // We need a new state for this or just hack it here?
                            // Better to use state. Let's create a temporary composable dialog here or manage state properly.
                            // Since we are inside a callback, we can't emit a new composable easily without state.
                            // BUT wait, we can't change state and expect the UI to show a new dialog if we just set value=null.
                            // WE NEED A NEW STATE VARIABLE for "showManualUninstallDialog".
                            
                            // To avoid huge refactor, we will leverage the existing callback structure but we need a state.
                            // Let's rely on a secondary state that triggering this block sets.
                            
                            // HACK/FIX: We'll modify the `distroToUninstall` usage logic above to support a second step, OR we accept that we need to add a state variable at the top of HomeScreen.
                            // Let's add the state variable at the top called `manualUninstallDistro`.
                            // See the next `replace_file_content` for that insertion.
                            
                            // For this block, we sets the NEW state variable and clear the old one.
                            manualUninstallDistro.value = distro
                            
                        } else {
                            // --- STANDARD PROOT FLOW ---
                            val intent = TermuxIntentFactory.buildUninstallIntent(distro.id)
                            try {
                                onStartService(intent)
                                StateManager.setDistroInstalled(context, distro.id, false)
                                android.widget.Toast.makeText(context, "Uninstalling ${distro.name}...", android.widget.Toast.LENGTH_SHORT).show()
                                refreshKey.value++
                            } catch (e: Exception) {
                                android.util.Log.e("FluxLinux", "Uninstall failed", e)
                            }
                            distroToUninstall.value = null
                        }
                    }
                ) { Text("Uninstall", color = FluxAccentMagenta) }
            },
            dismissButton = {
                TextButton(onClick = { distroToUninstall.value = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            }
        )
    }

    // Manual Chroot Uninstall Instruction Dialog
    if (manualUninstallDistro.value != null) {
        val distro = manualUninstallDistro.value!!
        AlertDialog(
            onDismissRequest = { manualUninstallDistro.value = null },
            title = { Text("Manual Root Required", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("To uninstall this Chroot environment, you must use Root (superuser) access manually.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("1. Click 'Proceed' to Copy Command & Open Termux.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("2. In Termux, type 'su' and press Enter.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("3. Paste the command and run it.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("The app will detect when uninstallation is complete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val scriptName = when(distro.id) {
                            "debian_chroot" -> "chroot/uninstall_debian_chroot.sh"
                            "debian13_chroot" -> "chroot/uninstall_debian13.sh"
                            else -> "chroot/uninstall_debian_chroot.sh"
                        }
                        
                        try {
                            val scriptManager = ScriptManager(context)
                            val scriptContent = scriptManager.getScriptContent(scriptName)
                            val command = TermuxIntentFactory.getSafeRootManualCommand(scriptContent, "uninstall_${distro.id}.sh")
                            
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FluxLinux Uninstall", command)
                            clipboard.setPrimaryClip(clip)
                            
                            val launchIntent = TermuxIntentFactory.buildOpenTermuxIntent(context)
                            if (launchIntent != null) {
                                onStartActivity(launchIntent)
                                android.widget.Toast.makeText(context, "Command Copied! Type 'su' -> Enter -> Paste", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "Termux app not found!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Error preparing script: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        manualUninstallDistro.value = null
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { manualUninstallDistro.value = null }) {
                    Text("Cancel")
                }
            }
        )
    }

}


