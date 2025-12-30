package com.ivarna.fluxlinux.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.fluxlinux.core.data.Distro
import com.ivarna.fluxlinux.core.data.DistroComponent
import com.ivarna.fluxlinux.core.utils.StateManager
import com.ivarna.fluxlinux.ui.components.GlassScaffold
import com.ivarna.fluxlinux.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun DistroSettingsScreen(
    distro: Distro,
    onBack: () -> Unit,
    onInstallComponent: (DistroComponent) -> Unit,
    onUninstallDistro: () -> Unit,
    onNavigateToStart: (() -> Unit)? = null,
    onStartActivity: (android.content.Intent) -> Unit,
    hazeState: HazeState
) {
    val context = LocalContext.current
    var showUninstallDialog by remember { mutableStateOf(false) }

    GlassScaffold(
        hazeState = hazeState,
        topBar = {
            TopAppBar(
                title = { Text("Manage ${distro.name}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.hazeChild(state = hazeState, shape = androidx.compose.ui.graphics.RectangleShape, style = HazeMaterials.thin())
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                
                // Header (Adaptive Card)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(distro.color.copy(alpha = 0.15f))
                            .border(1.dp, distro.color.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = distro.color,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(distro.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Status: Installed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                // Distro Components
                item {
                    Text(
                        text = "Features & Components",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(distro.components) { component ->
                    val isInstalled = remember(component.id) { 
                        StateManager.isComponentInstalled(context, distro.id, component.id) 
                    }
                    
                    ComponentSettingItem(
                        component = component,
                        isInstalled = isInstalled,
                        onAction = { onInstallComponent(component) }
                    )
                }

                // Danger Zone
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Danger Button (Adaptive Style)
                    Button(
                        onClick = { showUninstallDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uninstall ${distro.name}", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    )
    
    // Uninstall Dialog Logic
    if (showUninstallDialog) {
        val isChroot = distro.id == "debian_chroot" || distro.id == "debian13_chroot" || distro.id == "arch_chroot" || distro.id.contains("chroot")
        
        if (isChroot) {
             AlertDialog(
                onDismissRequest = { showUninstallDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = { Text("Manual Root Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        Text("To uninstall this Chroot environment, you must use Root (superuser) access manually.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("1. Click 'Proceed' to Copy Command & Open Termux.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("2. In Termux, type 'su' and press Enter.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("3. Paste the command and run it.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("The app will detect when uninstallation is complete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                             // ... (Existing logic for script generation) ...
                            val scriptName = when(distro.id) {
                                "debian_chroot" -> "chroot/uninstall_debian_chroot.sh"
                                "debian13_chroot" -> "chroot/uninstall_debian13.sh"
                                else -> "chroot/uninstall_debian_chroot.sh"
                            }
                            
                            try {
                                val scriptManager = com.ivarna.fluxlinux.core.data.ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent(scriptName)
                                val command = com.ivarna.fluxlinux.core.data.TermuxIntentFactory.getSafeRootManualCommand(scriptContent, "uninstall_${distro.id}.sh")
                                
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("FluxLinux Uninstall", command)
                                clipboard.setPrimaryClip(clip)
                                
                                val launchIntent = com.ivarna.fluxlinux.core.data.TermuxIntentFactory.buildOpenTermuxIntent(context)
                                if (launchIntent != null) {
                                    onStartActivity(launchIntent)
                                    android.widget.Toast.makeText(context, "Command Copied! Type 'su' -> Enter -> Paste", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Termux app not found!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error preparing script: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            showUninstallDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Proceed")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUninstallDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showUninstallDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = { Text("Uninstall ${distro.name}?", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("This will remove all data and files for ${distro.name}. This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUninstallDialog = false
                            onUninstallDistro()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Uninstall")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUninstallDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun ComponentSettingItem(
    component: DistroComponent,
    isInstalled: Boolean,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isInstalled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Installed", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInstalled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isInstalled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isInstalled) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update", fontSize = 12.sp)
                } else {
                    Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
