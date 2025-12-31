package com.ivarna.fluxlinux.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
    onInstallComponent: (DistroComponent, Map<String, String>) -> Unit,
    onUninstallDistro: () -> Unit,
    onReinstallDistro: () -> Unit,
    onNavigateToStart: (() -> Unit)? = null,
    onStartActivity: (android.content.Intent) -> Unit,
    hazeState: HazeState
) {
    val context = LocalContext.current
    var showUninstallDialog by remember { mutableStateOf(false) }
    
    // Config Dialog States
    var showThemeDialog by remember { mutableStateOf(false) }
    var showGpuDialog by remember { mutableStateOf(false) }
    var activeComponent by remember { mutableStateOf<DistroComponent?>(null) }
    
    // Selections
    var selectedTheme by remember { mutableStateOf("dark") }
    var selectedGpu by remember { mutableStateOf("auto") }

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
                    val details = componentDetailsMap[component.id]
                    
                    ComponentManagementCard(
                        component = component,
                        isInstalled = isInstalled,
                        details = details,
                        onAction = { 
                            if (component.scriptName.contains("setup_customization")) {
                                activeComponent = component
                                showThemeDialog = true
                            } else if (component.scriptName.contains("setup_hw_accel")) {
                                activeComponent = component
                                showGpuDialog = true
                            } else {
                                onInstallComponent(component, emptyMap()) 
                            }
                        }
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
                    
                    // Reinstall Button (Redo Base Install)
                    Button(
                        onClick = onReinstallDistro,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Redo Base Installation", color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
                                    // Optimistic State Update: Assume user will run the command
                                    StateManager.setDistroInstalled(context, distro.id, false)
                                    onStartActivity(launchIntent)
                                    // Navigate back to Home
                                    onBack() 
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

    // Theme Configuration Dialog
    if (showThemeDialog && activeComponent != null) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Configure Customization", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a theme to apply:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingsThemeOption(name = "Dark Mode (Default)", desc = "Sleek and professional.", id = "dark", selected = selectedTheme == "dark", onSelect = { selectedTheme = "dark" })
                    SettingsThemeOption(name = "Light Mode", desc = "Clean and bright.", id = "light", selected = selectedTheme == "light", onSelect = { selectedTheme = "light" })
                }
            },
            confirmButton = {
                Button(onClick = {
                    showThemeDialog = false
                    onInstallComponent(activeComponent!!, mapOf("FLUX_THEME" to selectedTheme))
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // GPU Configuration Dialog
    if (showGpuDialog && activeComponent != null) {
        AlertDialog(
            onDismissRequest = { showGpuDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Configure Hardware Acceleration", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select acceleration mode:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingsThemeOption(name = "Auto Detect (Recommended)", desc = "Detects Snapdragon (Turnip) or uses VirGL.", id = "auto", selected = selectedGpu == "auto", onSelect = { selectedGpu = "auto" })
                    SettingsThemeOption(name = "VirGL (Universal)", desc = "Compatible with most devices.", id = "virgl", selected = selectedGpu == "virgl", onSelect = { selectedGpu = "virgl" })
                    SettingsThemeOption(name = "Turnip/Zink (Snapdragon)", desc = "High performance for Adreno.", id = "turnip", selected = selectedGpu == "turnip", onSelect = { selectedGpu = "turnip" })
                    SettingsThemeOption(name = "Force Re-Detect", desc = "Ask interactively during install.", id = "ask", selected = selectedGpu == "ask", onSelect = { selectedGpu = "ask" })
                }
            },
            confirmButton = {
                Button(onClick = {
                    showGpuDialog = false
                    onInstallComponent(activeComponent!!, mapOf("FLUX_GPU" to selectedGpu))
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpuDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

@Composable
fun SettingsThemeOption(name: String, desc: String, id: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
        )
        Column {
            Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ComponentManagementCard(
    component: DistroComponent,
    isInstalled: Boolean,
    details: ComponentDetail?,
    onAction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded } // Toggle expand on body click
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top // Align top for better layout with description
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (details != null) {
                            Icon(
                                imageVector = details.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
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
                     Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Est. Size: ${component.sizeEstimate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Action Button (Right aligned)
               Column(
                   horizontalAlignment = Alignment.CenterHorizontally,
                   verticalArrangement = Arrangement.Center
               ) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInstalled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary,
                            contentColor = if (isInstalled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isInstalled) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Update", fontSize = 12.sp)
                        } else {
                            Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (details != null) {
                         IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
               }
            }
            
            // Collapsible Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (details != null) {
                    Column(modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Package Contents:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        details.packages.forEach { (pkg, size) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $pkg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = size,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
