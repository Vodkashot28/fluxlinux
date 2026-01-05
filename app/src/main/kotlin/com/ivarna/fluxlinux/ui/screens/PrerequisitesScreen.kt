package com.ivarna.fluxlinux.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.ivarna.fluxlinux.core.utils.StateManager
import com.ivarna.fluxlinux.core.utils.RootUtils
import com.ivarna.fluxlinux.core.utils.SystemInfoUtils
import com.ivarna.fluxlinux.core.data.TermuxIntentFactory
import com.ivarna.fluxlinux.core.data.ScriptManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.filled.Refresh
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PrerequisitesScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Step tracking
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 9 // Merging logical steps 5 & 6 visually
    
    // Package states
    val termuxInstalled = remember { mutableStateOf(StateManager.isTermuxInstalled(context)) }
    val x11Installed = remember { mutableStateOf(StateManager.isTermuxX11Installed(context)) }
    

    
    // Configuration state
    var configDone by remember { mutableStateOf(false) }
    
    // Permission state
    val permissionState = rememberPermissionState(
        permission = "com.termux.permission.RUN_COMMAND"
    )

    // Function to re-check status
    val checkStatus = {
        termuxInstalled.value = StateManager.isTermuxInstalled(context)
        x11Installed.value = StateManager.isTermuxX11Installed(context)
    }

    // Refresh when app resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Title
            Text(
                text = "Prerequisites",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "FluxLinux requires these to function",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Step Indicator
            // Logical step 6 (Environment Setup) shares the visual step 5 slot
            val visualStep = if (currentStep > 5) currentStep - 1 else currentStep
            StepIndicator(currentStep = visualStep, totalSteps = totalSteps)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Content based on current step
            when (currentStep) {
                1 -> PackageInstallationStep(
                    termuxInstalled = termuxInstalled,
                    x11Installed = x11Installed,
                    onRefresh = checkStatus,
                    onContinue = {
                        if (termuxInstalled.value && x11Installed.value) {
                            currentStep = 2
                        }
                    }
                )
                
                2 -> TermuxConfigurationStep(
                    configDone = configDone,
                    onConfigDone = { configDone = it },
                    onContinue = {
                        if (configDone) {
                            StateManager.setConnectionFixed(context, true)
                            currentStep = 3
                        }
                    }
                )
                
                3 -> PermissionRequestStep(
                    permissionState = permissionState,
                    onContinue = { currentStep = 4 }
                )
                
                4 -> OverlayPermissionStep(
                    onContinue = { currentStep = 5 }
                )

                5 -> PhantomProcessStep(
                    onContinue = { currentStep = 6 }
                )
                
                6 -> EnvironmentSetupStep(
                    onContinue = { currentStep = 7 }
                )
                
                7 -> SystemCheckStep(
                    onContinue = { currentStep = 8 }
                )
                
                8 -> KeyboardInstallStep(
                    onContinue = { currentStep = 9 }
                )
                
                9 -> OverlayKeyboardStep(
                    onContinue = { currentStep = 10 }
                )
                
                10 -> FinalInstructionsStep(
                    onComplete = onComplete
                )
            }
        }
    }
}

@Composable
fun PackageInstallationStep(
    termuxInstalled: MutableState<Boolean>,
    x11Installed: MutableState<Boolean>,
    onRefresh: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step 1: Install Required Apps",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Termux
        PrerequisiteItem(
            name = "Termux",
            isInstalled = termuxInstalled.value,
            version = if (termuxInstalled.value) StateManager.getTermuxVersion(context) else null,
            onInstall = {
                val url = "https://github.com/termux/termux-app/releases/tag/v0.118.3"
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Termux:X11
        PrerequisiteItem(
            name = "Termux:X11",
            isInstalled = x11Installed.value,
            version = if (x11Installed.value) StateManager.getTermuxX11Version(context) else null,
            onInstall = {
                val url = "https://github.com/termux/termux-x11/releases"
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = onContinue,
            enabled = termuxInstalled.value && x11Installed.value,
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Continue",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TermuxConfigurationStep(
    configDone: Boolean,
    onConfigDone: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val fixCommand = "mkdir -p ~/.termux && echo \"allow-external-apps = true\" >> ~/.termux/termux.properties && termux-reload-settings"
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 2: Configure Termux",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Allow FluxLinux to communicate with Termux",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f),
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Command Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = fixCommand,
                color = Color(0xFF50fa7b),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Copy & Open Button
        Button(
            onClick = {
                // Copy to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Termux Fix", fixCommand)
                clipboard.setPrimaryClip(clip)
                
                // Open Termux
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux")
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    android.widget.Toast.makeText(context, "Command copied! Paste in Termux", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Termux not found!", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Copy & Open Termux",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
                // Checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = configDone,
                onCheckedChange = onConfigDone,
                colors = CheckboxDefaults.colors(
                    checkedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    uncheckedColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "I've pasted and run the command in Termux",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = onContinue,
            enabled = configDone,
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Continue",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestStep(
    permissionState: com.google.accompanist.permissions.PermissionState,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 3: Grant Permission",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                .border(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Permission Lock",
                modifier = Modifier.size(56.dp),
                tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "FluxLinux needs permission to communicate with Termux",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f),
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Permission status
        if (permissionState.status.isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Permission Granted ✓",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Request Permission Button
            Button(
                onClick = {
                    permissionState.launchPermissionRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Grant Permission",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onTertiary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = onContinue,
            enabled = permissionState.status.isGranted,
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Next",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PrerequisiteItem(
    name: String,
    isInstalled: Boolean,
    version: String?,
    onInstall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInstalled) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Installed",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$name ✓",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (version != null) {
                        Text(
                            text = version,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Text(
                    text = name,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Download", color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val step = index + 1
            Box(
                modifier = Modifier
                    .size(if (step == currentStep) 12.dp else 8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (step <= currentStep) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                    )
            )
            if (step < totalSteps) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun OverlayPermissionStep(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    
    // Manual override (User confirmation)
    var manualOverride by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Step 4: Display Overlay",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

         // Permission Info Card
         androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
         ) {
             Column(modifier = Modifier.padding(20.dp)) {
                 Text(
                    "⚠️ Critical Permission",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error, // Usage error for warning
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                 )
                 Spacer(modifier = Modifier.height(8.dp))
                 Text(
                    "To display the Linux desktop (X11) on your screen, Termux needs the 'Display over other apps' permission.",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                 )
             }
         }
         
         Spacer(modifier = Modifier.height(24.dp))
         
         if (manualOverride) {
             // Success State
             Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { manualOverride = false }
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Permission Granted ✓",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
         } else {
             // Action Buttons
             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.spacedBy(12.dp)
             ) {
                 // Overlay Settings Button
                 Button(
                     onClick = { 
                         try {
                             val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                             intent.data = android.net.Uri.parse("package:com.termux")
                             context.startActivity(intent)
                         } catch (e: Exception) {
                              try {
                                 val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                 context.startActivity(intent)
                             } catch (e2: Exception) {
                                 Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                             }
                         }
                     },
                     colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
                     modifier = Modifier.weight(1f),
                     shape = RoundedCornerShape(12.dp)
                 ) {
                     Text("Enable Overlay", fontSize = 13.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center, lineHeight = 16.sp)
                 }
                 
                 // App Info Button
                 Button(
                     onClick = { 
                         try {
                             val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                             intent.data = android.net.Uri.parse("package:com.termux")
                             context.startActivity(intent)
                         } catch (e: Exception) {
                             Toast.makeText(context, "Could not open App Info", Toast.LENGTH_SHORT).show()
                         }
                     },
                     colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary),
                     modifier = Modifier.weight(1f),
                     shape = RoundedCornerShape(12.dp)
                 ) {
                     Text("App Info", fontSize = 13.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp)
                 }
             }
             
             Spacer(modifier = Modifier.height(16.dp))

             // Help Link
             androidx.compose.material3.TextButton(
                 onClick = { 
                     val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://support.google.com/android/answer/12623953?hl=en"))
                     context.startActivity(browserIntent)
                 }
             ) {
                 Text(
                     "How to allow restricted settings on Android devices",
                     color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                     fontSize = 13.sp,
                     textAlign = TextAlign.Center,
                     textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                 )
             }
             
             Spacer(modifier = Modifier.height(16.dp))
             
             // Manual Override Checkbox
             Row(
                 verticalAlignment = Alignment.CenterVertically,
                 modifier = Modifier
                     .fillMaxWidth()
                     .clickable { manualOverride = !manualOverride }
                     .padding(8.dp)
             ) {
                 Checkbox(
                     checked = manualOverride,
                     onCheckedChange = { manualOverride = it },
                     colors = CheckboxDefaults.colors(
                         checkedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                         uncheckedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)
                     )
                 )
                 Text(
                     "I have enabled this manually",
                     fontSize = 14.sp,
                     color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                     modifier = Modifier.padding(start = 8.dp)
                 )
             }
         }
         
         Spacer(modifier = Modifier.weight(1f))
         
         // Continue Button
         Button(
            onClick = onContinue,
            enabled = manualOverride,
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Next",
                color = if (manualOverride) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ColumnScope.PhantomProcessStep(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var rootAvailable by remember { mutableStateOf<Boolean?>(null) }
    var fixApplied by remember { mutableStateOf(false) }
    var checkingRoot by remember { mutableStateOf(false) }
    
    // Check root on init
    LaunchedEffect(Unit) {
        checkingRoot = true
        kotlinx.coroutines.delay(500) // fake delay for UX
        rootAvailable = RootUtils.isRootAvailable()
        checkingRoot = false
    }

    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = "Step 5: Process Killer Fix",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info Card
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "⚠️ Android 12+ Stability Issue",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Android 12 and higher kill background processes aggressively (Phantom Process Killer). This causes Termux to crash unexpectedly.",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (checkingRoot) {
            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Checking for Root access...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
        } else if (fixApplied) {
            // Success State
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Applied",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Fix Applied Successfully ✓",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (rootAvailable == true) {
            // Root Available Action
            Text(
                "Root Access Detected ✅",
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val r1 = RootUtils.runRootCommand("/system/bin/device_config set_sync_disabled_for_tests persistent")
                        val r2 = RootUtils.runRootCommand("/system/bin/device_config put activity_manager max_phantom_processes 2147483647")
                        val r3 = RootUtils.runRootCommand("settings put global settings_enable_monitor_phantom_procs false")
                        
                        if (r1.isSuccess && r2.isSuccess && r3.isSuccess) {
                           fixApplied = true 
                        } else {
                           Toast.makeText(context, "Failed to apply fix: ${r1.error}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                 shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Fix (Grant Root)", color = androidx.compose.material3.MaterialTheme.colorScheme.onTertiary)
            }
        } else {
            // No Root
             Text(
                "Root Access Not Detected ❌",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We cannot apply the fix automatically. Please run these commands from your PC via ADB:",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // ADB Commands Box
            val commands = """
                adb shell "/system/bin/device_config set_sync_disabled_for_tests persistent"
                adb shell "/system/bin/device_config put activity_manager max_phantom_processes 2147483647"
                adb shell "settings put global settings_enable_monitor_phantom_procs false"
            """.trimIndent()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ADB Commands", commands)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Commands copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Text(
                    text = commands,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
             // Re-check Button
             androidx.compose.material3.OutlinedButton(
                 onClick = { 
                     checkingRoot = true
                     coroutineScope.launch {
                         kotlinx.coroutines.delay(500)
                         rootAvailable = RootUtils.isRootAvailable()
                         checkingRoot = false
                     }
                 },
                 modifier = Modifier.fillMaxWidth(),
                 colors = ButtonDefaults.outlinedButtonColors(
                     contentColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                 ),
                 border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.secondary)
             ) {
                 Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("Check Root Again")
             }
        }
        
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Continue / Skip
        Button(
            onClick = onContinue,
            // Always enabled, user can skip if they want/have to
            enabled = true,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (fixApplied) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (fixApplied) "Next" else "Skip (Use ADB instead)",
                color = if (fixApplied) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EnvironmentSetupStep(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scriptManager = remember { ScriptManager(context) }
    
    // State to track if scripts have been run
    // Initialize from StateManager to handle screen rotation/re-entry
    var setupInitiated by remember { mutableStateOf(StateManager.getScriptStatus(context, "setup_termux")) }
    var isSetupLoading by remember { mutableStateOf(false) }
    var tweaksInitiated by remember { mutableStateOf(false) }
    var tweaksCompleted by remember { mutableStateOf(StateManager.getScriptStatus(context, "termux_tweaks")) }
    
    // Poll for setup completion
    LaunchedEffect(isSetupLoading) {
        if (isSetupLoading) {
            while (isSetupLoading) {
                delay(2000) // Check every 2 seconds
                if (StateManager.getScriptStatus(context, "setup_termux")) {
                    isSetupLoading = false
                    setupInitiated = true
                    Toast.makeText(context, "Environment Initialized Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Poll for tweaks completion
    LaunchedEffect(tweaksInitiated) {
        if (tweaksInitiated && !tweaksCompleted) {
            while (!tweaksCompleted) {
                delay(2000) // Check every 2 seconds
                if (StateManager.getScriptStatus(context, "termux_tweaks")) {
                     tweaksCompleted = true
                     break
                }
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Environment Setup",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please run both scripts below to set up your environment.",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "1. Initialize Environment: Sets up the core Linux system.\n2. Apply Termux Tweaks: Configures the shell and visuals (Optional).",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Setup Button (Background)
        Button(
            onClick = {
                val script = scriptManager.getScriptContent("common/setup_termux.sh")
                // Reset status first
                StateManager.setScriptStatus(context, "setup_termux", false)
                
                val intent = TermuxIntentFactory.buildRunCommandIntent(script, runInBackground = false)
                try {
                    context.startService(intent)
                    isSetupLoading = true
                    Toast.makeText(context, "Opening Termux...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to start service", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (setupInitiated) androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                contentColor = if (setupInitiated) androidx.compose.material3.MaterialTheme.colorScheme.secondary else androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !isSetupLoading && !setupInitiated,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSetupLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Initializing... (Wait for Termux)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (setupInitiated) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Environment Initialized",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "1. Initialize Environment (Required)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tweaks Button (Foreground)
        Button(
            onClick = {
                val script = scriptManager.getScriptContent("common/termux_tweaks.sh")
                val intent = TermuxIntentFactory.buildRunCommandIntent(script, runInBackground = false)
                try {
                    context.startService(intent)
                    tweaksInitiated = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to start service", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = setupInitiated && !tweaksCompleted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (tweaksCompleted) androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                disabledContainerColor = if (tweaksCompleted) androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha=0.5f),
                disabledContentColor = if (tweaksCompleted) androidx.compose.material3.MaterialTheme.colorScheme.secondary else androidx.compose.material3.MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                 if (tweaksCompleted) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tweaks Applied",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "2. Apply Termux Tweaks (Optional)",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onTertiary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (tweaksInitiated) {
                             Text(
                                "(Opened in Termux... Wait for completion)",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onTertiary.copy(alpha=0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = onContinue,
            // Enforce only core setup
            enabled = setupInitiated,  
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Next",
                color = if (setupInitiated) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FinalInstructionsStep(
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 9: Almost Done!",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Warning Card
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Important Note",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "FluxLinux runs on top of Termux.",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You must keep Termux running in the background. Do not swipe close Termux from your recent apps!",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Complete Button
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Complete Setup",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SystemCheckStep(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val memoryInfo = remember { SystemInfoUtils.getMemoryInfo(context) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 6: System Check",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Checking your system resources",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // RAM Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💾",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "System RAM",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%.2f GB".format(memoryInfo.totalRamGB),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // RAM Info/Warning based on amount
            when {
                memoryInfo.totalRamGB >= 7f -> {
                    // Good RAM, show recommendation
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "✓ Good RAM",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your RAM is sufficient. For optimal performance, 12GB RAM would be great!",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                memoryInfo.totalRamGB < 6f -> {
                    // Critical low RAM
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "🚨 CRITICAL: Low RAM",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Minimum required RAM: 8GB\nYour system will experience severe performance issues and instability.",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                // Between 6GB and 7GB - no message shown
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // SWAP Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔄",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "System SWAP",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%.2f GB".format(memoryInfo.totalSwapGB),
                        color = if (memoryInfo.totalSwapGB <= 7.9f) androidx.compose.material3.MaterialTheme.colorScheme.error else androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // SWAP Critical Warning if < 8GB (with tolerance for floating point precision)
            if (memoryInfo.totalSwapGB <= 7.9f) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "🚨 CRITICAL: Low SWAP",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Minimum required SWAP: 8GB\nWithout sufficient SWAP, your Linux environment will be UNSTABLE and may crash.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.8f),
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Enable more SWAP in system settings", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go to Settings", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button (always enabled - not a hard requirement)
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Next",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun KeyboardInstallStep(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    
    // Keyboard installation state
    val keyboardInstalled = remember { mutableStateOf(StateManager.isUnexpectedKeyboardInstalled(context)) }
    val keyboardProgress = remember { mutableStateOf(0f) }
    
    // Refresh when app resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                keyboardInstalled.value = StateManager.isUnexpectedKeyboardInstalled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 7: Install Keyboard",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "⌨️",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Install Unexpected Keyboard for better Linux terminal experience",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Keyboard Installation Card
        PrerequisiteItem(
            name = "Unexpected Keyboard",
            isInstalled = keyboardInstalled.value,
            version = if (keyboardInstalled.value) StateManager.getUnexpectedKeyboardVersion(context) else null,

            onInstall = {
                val url = "https://github.com/Julow/Unexpected-Keyboard/releases"
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "ℹ️ This keyboard is optimized for terminal use with special keys and shortcuts. Installation is optional but recommended.",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f),
                fontSize = 13.sp
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button (always enabled - not a hard requirement)
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Next",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OverlayKeyboardStep(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    
    // Check overlay permission for FluxLinux app
    var hasOverlayPermission by remember { 
        mutableStateOf(android.provider.Settings.canDrawOverlays(context)) 
    }
    
    // Check if floating keyboard service is running
    var isServiceRunning by remember { mutableStateOf(false) }
    
    // Refresh permission status on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Step 8: Floating Keyboard Button",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Permission Info Card
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "⌨️ Floating Keyboard Toggle",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "A floating button will appear on your screen to quickly toggle the keyboard on/off. You can drag it to any edge and remove it by dragging to the bottom.",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (hasOverlayPermission) {
            // Success State
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Overlay Permission Granted ✓",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enable/Disable Floating Button
            Button(
                onClick = {
                    if (!isServiceRunning) {
                        // Start floating keyboard service
                        try {
                            val intent = android.content.Intent(context, com.ivarna.fluxlinux.core.services.FloatingKeyboardService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            isServiceRunning = true
                            Toast.makeText(context, "Floating keyboard button enabled", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to start service: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Stop service
                        try {
                            val intent = android.content.Intent(context, com.ivarna.fluxlinux.core.services.FloatingKeyboardService::class.java)
                            context.stopService(intent)
                            isServiceRunning = false
                            Toast.makeText(context, "Floating keyboard button disabled", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to stop service: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceRunning) androidx.compose.material3.MaterialTheme.colorScheme.error else androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                    contentColor = if (isServiceRunning) androidx.compose.material3.MaterialTheme.colorScheme.onError else androidx.compose.material3.MaterialTheme.colorScheme.onTertiary
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isServiceRunning) "Disable Floating Button" else "Enable Floating Button",
                    fontSize = 16.sp
                )
            }
        } else {
            // Request Permission
            Button(
                onClick = { 
                    try {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Overlay Permission", fontSize = 16.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onTertiary)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Next",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
