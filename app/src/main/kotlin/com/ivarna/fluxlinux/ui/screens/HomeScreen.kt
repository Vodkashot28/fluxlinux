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
import androidx.compose.material.icons.filled.PlayArrow
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
    onStartActivity: (android.content.Intent) -> Unit,
    onNavigateToInstall: (com.ivarna.fluxlinux.core.data.Distro) -> Unit,
    onNavigateToSettings: (com.ivarna.fluxlinux.core.data.Distro) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State for Launch Popup
    val distroToLaunch = remember { mutableStateOf<com.ivarna.fluxlinux.core.data.Distro?>(null) }
    
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
                color = MaterialTheme.colorScheme.secondary
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
                            containerColor = if (isFloatingKeyboardRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (isFloatingKeyboardRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (isFloatingKeyboardRunning) "Disable" else "Enable",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Permission", fontSize = 12.sp)
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
            color = MaterialTheme.colorScheme.secondary,
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
                    onInstall = { onNavigateToInstall(distro) },
                    onUninstall = { /* Handled in Settings */ }, 
                    onNavigateToSettings = { onNavigateToSettings(distro) },
                    onNavigateToStart = { distroToLaunch.value = distro }
                )
            }
        }
    }

    
    Spacer(modifier = Modifier.height(100.dp))
    
    // Launch Popup
    if (distroToLaunch.value != null) {
        val distro = distroToLaunch.value!!
        AlertDialog(
            onDismissRequest = { distroToLaunch.value = null },
            title = { 
                Text(
                    "Start ${distro.name}", 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = { Text("Choose how you want to launch the distribution.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { distroToLaunch.value = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface) 
                }
            },
            icon = {
                 Icon(
                     imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                     contentDescription = null,
                     tint = FluxAccentCyan
                 )
            },
            // Custom Layout for Buttons
            // Using a Row with two big buttons? LIMITATION: AlertDialog has specific slots.
            // We can put the buttons in the "text" part or just use confirm/dismiss as actions?
            // Better to use the text part to house the buttons for vertical stacking or a Row.
        )
        // AlertDialog is a bit restrictive for 2 "positive" actions.
        // Let's use a custom Dialog or just use the Buttons in the text area?
        // Actually, we can just put a Column in the 'text' slot.
    }
    
    // Custom Launch Dialog (replacing the standard AlertDialog above for better control)
    if (distroToLaunch.value != null) {
        val distro = distroToLaunch.value!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { distroToLaunch.value = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Start ${distro.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // CLI Button
                    if (distro.id != "termux") {
                        Button(
                            onClick = {
                                if (permissionState.status.isGranted) {
                                    val intent = TermuxIntentFactory.buildLaunchCliIntent(distro.id)
                                    try {
                                        onStartService(intent)
                                        distroToLaunch.value = null
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Launch failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    permissionState.launchPermissionRequest()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Launch CLI", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // GUI Button
                    Button(
                        onClick = {
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
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                
                                val intent = TermuxIntentFactory.buildLaunchGuiIntent(distro.id)
                                try {
                                    onStartService(intent)
                                    distroToLaunch.value = null
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Launch failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00E6)),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                         Text("Launch GUI", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    TextButton(onClick = { distroToLaunch.value = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}


