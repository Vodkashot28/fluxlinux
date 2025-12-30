package com.ivarna.fluxlinux.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivarna.fluxlinux.core.data.DistroRepository
import com.ivarna.fluxlinux.core.data.Distro
import com.ivarna.fluxlinux.core.data.ScriptManager
import com.ivarna.fluxlinux.core.data.TermuxIntentFactory
import com.ivarna.fluxlinux.core.utils.StateManager
import com.ivarna.fluxlinux.ui.components.DistroCard
import com.ivarna.fluxlinux.ui.theme.FluxAccentMagenta
import com.ivarna.fluxlinux.ui.theme.GlassWhiteMedium
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DistroScreen(
    permissionState: PermissionState,
    hazeState: HazeState,
    onStartService: (android.content.Intent) -> Unit,
    onStartActivity: (android.content.Intent) -> Unit
) {
    val context = LocalContext.current
    
    // State for Uninstall Dialog
    val distroToUninstall = remember { mutableStateOf<com.ivarna.fluxlinux.core.data.Distro?>(null) }
    // State for Install Dialog
    val distroToInstall = remember { mutableStateOf<com.ivarna.fluxlinux.core.data.Distro?>(null) }
    
    // Refresh mechanism to check install status
    val refreshKey = remember { mutableStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshKey.value++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Available Distros",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Distro List
        val installedDistroIds = remember(refreshKey.value) {
            StateManager.getInstalledDistros(context)
        }
        
        val availableDistros = DistroRepository.supportedDistros.filter { 
            !installedDistroIds.contains(it.id)
        }.sortedWith(compareBy<Distro> { it.comingSoon }.thenBy { it.name })
        
        if (availableDistros.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All available distros are installed!",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            availableDistros.forEach { distro ->
                if (distro.comingSoon) {
                    // Use compact card for coming soon distros
                    com.ivarna.fluxlinux.ui.components.CompactDistroCard(
                        distro = distro
                    )
                } else {
                    // Use full card for available distros
                    com.ivarna.fluxlinux.ui.components.DistroCard(
                        distro = distro,
                        isInstalled = false,
                        onInstall = {
                            if (permissionState.status.isGranted) {
                                distroToInstall.value = distro
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        },
                        onUninstall = {}, // Not used
                        onLaunchCli = {}, // Not used
                        onLaunchGui = {}, // Not used
                        onAppDevInstall = if (distro.id == "debian" || distro.id == "debian_chroot" || distro.id == "debian13_chroot") {
                            {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_appdev_debian.sh")
                                val scriptName = "setup_appdev_debian.sh"

                                val command = if (distro.id.contains("chroot")) {
                                    // Chroot Command
                                    val safeScript = if (!scriptContent.endsWith("\n")) "$scriptContent\n" else scriptContent
                                    val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.DEFAULT)
                                    val chunkedEchos = "cat << 'EOF_B64' > \"\${S}.b64\"\n$scriptB64\nEOF_B64\n"
                                    """
                                        S="/data/local/tmp/$scriptName"
                                        if [ "${'$'}(id -u)" != "0" ]; then S="${'$'}HOME/$scriptName"; fi
                                        $chunkedEchos
                                        base64 -d "${'$'}S.b64" > "${'$'}S"
                                        rm -f "${'$'}S.b64"
                                        chmod +x "${'$'}S"
                                        if [ "${'$'}(id -u)" = "0" ]; then
                                            # Execute inside chroot
                                            TARGET_SCRIPT="/opt/$scriptName"
                                            cp "${'$'}S" "${'$'}TARGET_SCRIPT"
                                            chmod +x "${'$'}TARGET_SCRIPT"
                                            chroot /data/local/tmp/chrootdebian /bin/bash -c "/opt/$scriptName"
                                        else
                                            echo "⚠️ PLEASE RUN AS ROOT ⚠️"
                                        fi
                                    """.trimIndent()
                                } else {
                                    // Proot Command (Simplified)
                                    val safeScript = if (!scriptContent.endsWith("\n")) "$scriptContent\n" else scriptContent
                                    val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.DEFAULT)
                                     """
                                        echo "$scriptB64" | base64 -d > ~/setup_appdev.sh
                                        chmod +x ~/setup_appdev.sh
                                        ~/setup_appdev.sh
                                    """.trimIndent()
                                }

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("FluxLinux App Dev", command)
                                clipboard.setPrimaryClip(clip)
                                
                                val launchIntent = TermuxIntentFactory.buildOpenTermuxIntent(context)
                                if (launchIntent != null) {
                                    onStartActivity(launchIntent)
                                    android.widget.Toast.makeText(context, "App Dev Script Copied! Paste in Termux.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        } else null,
                        appDevDescription = if (distro.id == "debian" || distro.id == "debian_chroot" || distro.id == "debian13_chroot") 
                            "Installs Android SDK, Flutter, JDK, IntelliJ, and more." else null,

                        onGenDevInstall = if (distro.id == "debian" || distro.id == "debian_chroot" || distro.id == "debian13_chroot") {
                            {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_gengdev_debian.sh")
                                val scriptName = "setup_gengdev_debian.sh"

                                val command = if (distro.id.contains("chroot")) {
                                    // Chroot Command
                                    val safeScript = if (!scriptContent.endsWith("\n")) "$scriptContent\n" else scriptContent
                                    val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.DEFAULT)
                                    val chunkedEchos = "cat << 'EOF_B64' > \"\${S}.b64\"\n$scriptB64\nEOF_B64\n"
                                    """
                                        S="/data/local/tmp/$scriptName"
                                        if [ "${'$'}(id -u)" != "0" ]; then S="${'$'}HOME/$scriptName"; fi
                                        $chunkedEchos
                                        base64 -d "${'$'}S.b64" > "${'$'}S"
                                        rm -f "${'$'}S.b64"
                                        chmod +x "${'$'}S"
                                        if [ "${'$'}(id -u)" = "0" ]; then
                                            # Execute inside chroot
                                            TARGET_SCRIPT="/opt/$scriptName"
                                            cp "${'$'}S" "${'$'}TARGET_SCRIPT"
                                            chmod +x "${'$'}TARGET_SCRIPT"
                                            chroot /data/local/tmp/chrootdebian /bin/bash -c "/opt/$scriptName"
                                        else
                                            echo "⚠️ PLEASE RUN AS ROOT ⚠️"
                                        fi
                                    """.trimIndent()
                                } else {
                                    // Proot Command
                                    val safeScript = if (!scriptContent.endsWith("\n")) "$scriptContent\n" else scriptContent
                                    val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.DEFAULT)
                                     """
                                        echo "$scriptB64" | base64 -d > ~/setup_gengdev.sh
                                        chmod +x ~/setup_gengdev.sh
                                        ~/setup_gengdev.sh
                                    """.trimIndent()
                                }

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("FluxLinux Gen Dev", command)
                                clipboard.setPrimaryClip(clip)
                                
                                val launchIntent = TermuxIntentFactory.buildOpenTermuxIntent(context)
                                if (launchIntent != null) {
                                    onStartActivity(launchIntent)
                                    android.widget.Toast.makeText(context, "Gen Dev Script Copied! Paste in Termux.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        } else null,
                        genDevDescription = if (distro.id == "debian" || distro.id == "debian_chroot" || distro.id == "debian13_chroot") 
                            "Installs Rust, Go, C++, LunarVim, VS Code & more." else null,
                            
                        onCustomize = if (distro.id == "debian" || distro.id == "debian_chroot" || distro.id == "debian13_chroot") {
                             {
                                val scriptManager = ScriptManager(context)
                                val scriptContent = scriptManager.getScriptContent("common/setup_customization_debian.sh")
                                val intent = TermuxIntentFactory.buildRunFeatureScriptIntent(distro.id, scriptContent)
                                try {
                                    onStartService(intent)
                                    android.widget.Toast.makeText(context, "Starting Desktop Customization...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to start customization", android.widget.Toast.LENGTH_SHORT).show()
                                }
                             }
                        } else null,
                        customizeDescription = if (distro.id == "debian" || distro.id == "debian_chroot" || distro.id == "debian13_chroot")
                            "Applies Themes, Fonts, Wallpapers & 2x Scaling." else null
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Spacing for Bottom Nav
    }
    
    // Install Confirmation Dialog
    if (distroToInstall.value != null) {
        val distro = distroToInstall.value!!
        AlertDialog(
            onDismissRequest = { distroToInstall.value = null },
            title = { Text("Install ${distro.name}?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("This will install ${distro.name} using PRoot. It may take some time depending on your internet connection.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = {
                        // 1. Get Setup Script
                        val scriptManager = ScriptManager(context)
                        val setupScript = if (distro.configuration?.family == com.ivarna.fluxlinux.core.model.DistroFamily.TERMUX) {
                            // Bundle all Termux scripts
                            val baseInstall = scriptManager.getScriptContent("termux/install.sh")
                            val appsInstall = scriptManager.getScriptContent("termux/install_apps.sh")
                            val themeInstall = scriptManager.getScriptContent("termux/setup_theme.sh")
                            val gpuInstall = scriptManager.getScriptContent("common/setup_gpu.sh")
                            val haWrapperContent = scriptManager.getScriptContent("common/ha")
                            
                            // Create header to write HA wrapper
                            val haInstallCmd = "cat << 'EOF_HA' > /data/data/com.termux/files/usr/bin/ha\n$haWrapperContent\nEOF_HA\nchmod +x /data/data/com.termux/files/usr/bin/ha\n"
                            
                            "$baseInstall\n$appsInstall\n$themeInstall\n$gpuInstall\n$haInstallCmd"
                        } else {
                            val scriptName = when (distro.configuration?.family) {
                                com.ivarna.fluxlinux.core.model.DistroFamily.DEBIAN -> "common/setup_debian_family.sh"
                                com.ivarna.fluxlinux.core.model.DistroFamily.ARCH -> "common/setup_arch_family.sh"
                                com.ivarna.fluxlinux.core.model.DistroFamily.FEDORA -> "common/setup_fedora_family.sh"
                                else -> "common/setup_debian_family.sh"
                            }
                            scriptManager.getScriptContent(scriptName)
                        }
                        
                        // 1b. Get Installer & GUI Scripts
                        val installScript = scriptManager.getScriptContent("common/flux_install.sh")
                        val guiScript = scriptManager.getScriptContent("common/start_gui.sh")
                        
                        // 2. Generate Command
                        val command = if (distro.id == "debian_chroot" || distro.id == "debian13_chroot" || distro.id == "arch_chroot") {
                            // Specialized Root Command for Chroot
                            // 2. Prepare Root Script
                            val scriptPath = when (distro.id) {
                                "arch_chroot" -> "chroot/setup_arch_chroot.sh"
                                "debian13_chroot" -> "chroot/setup_debian13_chroot.sh"
                                else -> "chroot/setup_debian_chroot.sh"
                            }
                            val scriptName = when (distro.id) {
                                "arch_chroot" -> "setup_arch_chroot.sh"
                                "debian13_chroot" -> "setup_debian13_chroot.sh"
                                else -> "setup_debian_chroot.sh"
                            }
                            
                            val chrootScript = scriptManager.getScriptContent(scriptPath)
                            val safeScript = if (!chrootScript.endsWith("\n")) "$chrootScript\n" else chrootScript
                            val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.DEFAULT)
                            
                            // Use Heredoc with wrapped Base64 to prevent terminal freeze (Line length limits)
                            val chunkedEchos = "cat << 'EOF_B64' > \"\${S}.b64\"\n$scriptB64\nEOF_B64\n"

                            // Simplified robust command
                            """
                                S="/data/local/tmp/$scriptName"
                                if [ "${'$'}(id -u)" != "0" ]; then S="${'$'}HOME/$scriptName"; fi
                                $chunkedEchos
                                base64 -d "${'$'}S.b64" > "${'$'}S"
                                rm -f "${'$'}S.b64"
                                chmod +x "${'$'}S"
                                if [ "${'$'}(id -u)" = "0" ]; then
                                    sh "${'$'}S"
                                else
                                    echo "⚠️ PLEASE RUN AS ROOT ⚠️"
                                    echo "Type su and press Enter."
                                    echo "Then paste this command again."
                                fi
                            """.trimIndent() + "\n"

                        } else {
                            // Standard Proot Command
                            TermuxIntentFactory.getInstallCommand(distro.id, setupScript, installScript, guiScript)
                        }
                        
                        // 3. Copy to Clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FluxLinux Install", command)
                        clipboard.setPrimaryClip(clip)
                        
                        // 4. Open Termux
                        val launchIntent = TermuxIntentFactory.buildOpenTermuxIntent(context)
                        if (launchIntent != null) {
                            try {
                                onStartActivity(launchIntent)
                                // Optimistic update removed - relying on script callback
                                // StateManager.setDistroInstalled(context, distro.id, true)
                                // refreshKey.value++ 
                                if (distro.id == "debian_chroot") {
                                    android.widget.Toast.makeText(context, "Copied! Type 'su' in Termux then Paste.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Command Copied! Paste in Termux.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FluxLinux", "Failed to open Termux", e)
                                android.widget.Toast.makeText(context, "Failed to open Termux", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Termux app not found!", android.widget.Toast.LENGTH_LONG).show()
                        }
                        
                        distroToInstall.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Install", color = MaterialTheme.colorScheme.onPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { distroToInstall.value = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            }
        )
    }

    // Uninstall Dialog
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
                ) { Text("Uninstall", color = FluxAccentMagenta) }
            },
            dismissButton = {
                TextButton(onClick = { distroToUninstall.value = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            }
        )
    }
}
