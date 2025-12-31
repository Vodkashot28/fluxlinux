package com.ivarna.fluxlinux.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.fluxlinux.core.data.Distro
import com.ivarna.fluxlinux.core.data.DistroComponent
import com.ivarna.fluxlinux.ui.components.GlassScaffold
import com.ivarna.fluxlinux.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import androidx.compose.foundation.clickable
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun InstallConfigScreen(
    distro: Distro,
    onBack: () -> Unit,
    onInstallStart: (List<DistroComponent>, String, String) -> Unit,
    hazeState: HazeState
) {
    var desktopEnv by remember { mutableStateOf("XFCE4") }
    var selectedTheme by remember { mutableStateOf("dark") } // dark, light, cyber
    var selectedGpu by remember { mutableStateOf("auto") } // auto, virgl, turnip, manual
    val selectedComponents = remember { mutableStateListOf<String>() }

    // Pre-select mandatory components
    LaunchedEffect(Unit) {
        distro.components.forEach {
            if (it.isMandatory) {
                selectedComponents.add(it.id)
            }
        }
    }

    GlassScaffold(
        hazeState = hazeState,
        topBar = {
            TopAppBar(
                title = { Text("Configure ${distro.name}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
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
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .hazeChild(state = hazeState, shape = RoundedCornerShape(24.dp), style = HazeMaterials.thin())
            ) {
                 Button(
                    onClick = {
                        val componentsToInstall = distro.components.filter { selectedComponents.contains(it.id) }
                        onInstallStart(componentsToInstall, selectedTheme, selectedGpu)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Install Now", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Section 1: Desktop Environment
            item {
                Text(
                    text = "1. Desktop Environment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Adaptive Card for DE Selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = desktopEnv == "XFCE4",
                                onClick = { desktopEnv = "XFCE4" },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Column {
                                Text("XFCE4", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Lightweight, fast, and stable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        // Placeholder for KDE
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = false,
                                onClick = {},
                                enabled = false,
                                colors = RadioButtonDefaults.colors(disabledUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            )
                            Column {
                                Text("KDE Plasma (Coming Soon)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text("Modern and customizable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
            
            // Section 2: Theme Selection (New)
            item {
                Text(
                    text = "2. Appearance Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption(name = "Dark Mode (Default)", desc = "Sleek and easy on eyes.", id = "dark", selected = selectedTheme == "dark", onSelect = { selectedTheme = "dark" })
                        ThemeOption(name = "Light Mode", desc = "Clean and bright.", id = "light", selected = selectedTheme == "light", onSelect = { selectedTheme = "light" })
                    }
                }
            }

            // Section 3: Graphics Acceleration (New)
            item {
                Text(
                    text = "3. Graphics Acceleration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption(name = "Auto Detect (Recommended)", desc = "Detects Snapdragon (Turnip) or uses VirGL.", id = "auto", selected = selectedGpu == "auto", onSelect = { selectedGpu = "auto" })
                        ThemeOption(name = "VirGL (Universal)", desc = "Compatible with most devices.", id = "virgl", selected = selectedGpu == "virgl", onSelect = { selectedGpu = "virgl" })
                        ThemeOption(name = "Turnip/Zink (Snapdragon)", desc = "High performance for Adreno.", id = "turnip", selected = selectedGpu == "turnip", onSelect = { selectedGpu = "turnip" })
                        ThemeOption(name = "Ask during Customization", desc = "Prompts you later.", id = "ask", selected = selectedGpu == "ask", onSelect = { selectedGpu = "ask" })
                    }
                }
            }

            // Section 4: Features
            item {
                Text(
                    text = "4. Select Features",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose add-ons to install. You can manage these later in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(distro.components) { component ->
                ComponentSelectionCard(
                    component = component,
                    isSelected = selectedComponents.contains(component.id),
                    onToggle = { isSelected ->
                        if (component.isMandatory) return@ComponentSelectionCard
                        if (isSelected) {
                            selectedComponents.add(component.id)
                        } else {
                            selectedComponents.remove(component.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ComponentSelectionCard(
    component: DistroComponent,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(enabled = !component.isMandatory) { onToggle(!isSelected) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null, // Handled by parent container click
                enabled = !component.isMandatory,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant, checkmarkColor = MaterialTheme.colorScheme.onPrimary)
            )
            
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (component.isMandatory) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Required", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Est. Size: ${component.sizeEstimate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun ThemeOption(name: String, desc: String, id: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Column {
            Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
