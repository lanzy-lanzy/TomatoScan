package com.ml.tomatoscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.app.Application
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ml.tomatoscan.viewmodels.UserViewModel
import com.ml.tomatoscan.viewmodels.UserViewModelFactory
import com.ml.tomatoscan.data.HistoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val userName by userViewModel.userName.collectAsState()
    val themeMode by userViewModel.themeMode.collectAsState()
    val appLanguage by userViewModel.appLanguage.collectAsState()
    val geminiPreValidationEnabled by userViewModel.geminiPreValidationEnabled.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = com.ml.tomatoscan.R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(id = com.ml.tomatoscan.R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Account Section
                SettingsSection(
                    title = stringResource(id = com.ml.tomatoscan.R.string.settings_section_account),
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = stringResource(id = com.ml.tomatoscan.R.string.settings_change_name),
                            subtitle = stringResource(id = com.ml.tomatoscan.R.string.settings_current_name, userName),
                            onClick = { showNameDialog = true }
                        )
                    )
                )

                // App Section
                SettingsSection(
                    title = stringResource(id = com.ml.tomatoscan.R.string.settings_section_application),
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Palette,
                            title = stringResource(id = com.ml.tomatoscan.R.string.settings_theme),
                            subtitle = when (themeMode) {
                                                            "light" -> stringResource(id = com.ml.tomatoscan.R.string.theme_light)
                                                            "dark" -> stringResource(id = com.ml.tomatoscan.R.string.theme_dark)
                                                            else -> stringResource(id = com.ml.tomatoscan.R.string.system_default)
                                                        },
                            onClick = { showThemeDialog = true }
                        ),
                        SettingsItem(
                            icon = Icons.Default.Language,
                            title = stringResource(id = com.ml.tomatoscan.R.string.settings_language),
                            subtitle = when (appLanguage) {
                                                            "en" -> stringResource(id = com.ml.tomatoscan.R.string.language_en)
                                                            "tl" -> stringResource(id = com.ml.tomatoscan.R.string.language_fil)
                                                            "ceb" -> stringResource(id = com.ml.tomatoscan.R.string.language_ceb)
                                                            else -> stringResource(id = com.ml.tomatoscan.R.string.system_default)
                                                        },
                            onClick = { showLanguageDialog = true }
                        )
                    )
                )

                // AI Features Section
                SettingsSection(
                    title = "AI Features",
                    items = listOf(
                        SettingsItemWithToggle(
                            icon = Icons.Default.AutoAwesome,
                            title = "Gemini Pre-Validation",
                            subtitle = if (geminiPreValidationEnabled) 
                                "AI verifies tomato leaf before analysis (requires internet)" 
                            else 
                                "Disabled - faster but may analyze non-tomato images",
                            isChecked = geminiPreValidationEnabled,
                            onToggle = { userViewModel.updateGeminiPreValidation(it) }
                        )
                    )
                )

                // Data Section
                SettingsSection(
                    title = stringResource(id = com.ml.tomatoscan.R.string.settings_section_data_privacy),
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Security,
                            title = stringResource(id = com.ml.tomatoscan.R.string.settings_privacy_policy),
                            subtitle = stringResource(id = com.ml.tomatoscan.R.string.settings_privacy_policy_sub),
                            onClick = { showPrivacyDialog = true }
                        ),
                        SettingsItem(
                            icon = Icons.Default.DeleteSweep,
                            title = stringResource(id = com.ml.tomatoscan.R.string.settings_clear_data),
                            subtitle = stringResource(id = com.ml.tomatoscan.R.string.settings_clear_data_sub),
                            onClick = {
                                coroutineScope.launch {
                                    // Clear Room history and preferences
                                    HistoryRepository(context).clearHistory()
                                    userViewModel.clearAllPreferences()
                                    // Recreate activity to apply defaults immediately
                                    (context as? Activity)?.recreate()
                                }
                            },
                            textColor = MaterialTheme.colorScheme.error,
                            hasNavigation = false
                        )
                    )
                )

                // About Section
                SettingsSection(
                    title = stringResource(id = com.ml.tomatoscan.R.string.settings_section_about),
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = stringResource(id = com.ml.tomatoscan.R.string.settings_about_title),
                            subtitle = stringResource(id = com.ml.tomatoscan.R.string.common_version, "1.0.0"),
                            onClick = { showAboutDialog = true }
                        )
                    )
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentSelection = themeMode,
            onSelect = { selection ->
                userViewModel.updateThemeMode(selection)
                // Recreate to apply immediately
                (context as? Activity)?.recreate()
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showNameDialog) {
        NameChangeDialog(
            currentName = userName,
            onDismiss = { showNameDialog = false },
            onConfirm = { newName ->
                userViewModel.updateUserName(newName)
                showNameDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentSelection = appLanguage,
            onSelect = { selection ->
                userViewModel.updateAppLanguage(selection)
                // Recreate to apply immediately
                (context as? Activity)?.recreate()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
fun SettingsSection(title: String, items: List<Any>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    when (item) {
                        is SettingsItem -> SettingsItemRow(item = item)
                        is SettingsItemWithToggle -> SettingsItemRowWithToggle(item = item)
                    }
                    if (index < items.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemRow(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(item.textColor?.copy(alpha = 0.1f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.textColor ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = item.textColor ?: MaterialTheme.colorScheme.onSurface
            )
            if (item.subtitle.isNotEmpty()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (item.hasNavigation) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = stringResource(id = com.ml.tomatoscan.R.string.settings_section_about)) },
        title = { Text(stringResource(id = com.ml.tomatoscan.R.string.settings_about_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(id = com.ml.tomatoscan.R.string.common_version, "1.0.0"), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(id = com.ml.tomatoscan.R.string.settings_about_app_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(id = com.ml.tomatoscan.R.string.settings_about_powered_by),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ThemeDialog(currentSelection: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Palette, contentDescription = "Theme") },
        title = { Text(stringResource(id = com.ml.tomatoscan.R.string.dialog_theme_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ThemeOption(stringResource(id = com.ml.tomatoscan.R.string.theme_system), currentSelection == "system") { onSelect("system"); onDismiss() }
                ThemeOption(stringResource(id = com.ml.tomatoscan.R.string.theme_light), currentSelection == "light") { onSelect("light"); onDismiss() }
                ThemeOption(stringResource(id = com.ml.tomatoscan.R.string.theme_dark), currentSelection == "dark") { onSelect("dark"); onDismiss() }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ThemeOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun LanguageDialog(currentSelection: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Language, contentDescription = "Language") },
        title = { Text(stringResource(id = com.ml.tomatoscan.R.string.dialog_language_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ThemeOption(stringResource(id = com.ml.tomatoscan.R.string.language_system), currentSelection == "system") { onSelect("system"); onDismiss() }
                ThemeOption(stringResource(id = com.ml.tomatoscan.R.string.language_en), currentSelection == "en") { onSelect("en"); onDismiss() }
                ThemeOption(stringResource(id = com.ml.tomatoscan.R.string.language_fil), currentSelection == "tl") { onSelect("tl"); onDismiss() }
                ThemeOption(stringResource(id = com.ml.tomatoscan.R.string.language_ceb), currentSelection == "ceb") { onSelect("ceb"); onDismiss() }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun NameChangeDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Person, contentDescription = "Change Name") },
        title = { Text("Change Your Name", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = "Privacy Policy") },
        title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "TomatoScan Privacy Policy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    "Last Updated: November 2025",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    "1. Data Collection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "TomatoScan collects and stores scan history locally on your device. Images are analyzed using machine learning technology for disease detection. We do not store your images on external servers.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "2. Image Processing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Images you capture are processed locally on your device for tomato disease analysis. Your images remain private and are not shared with third parties.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "3. Local Storage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "All scan results and user preferences are stored locally on your device. You can clear this data at any time from the Settings menu.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "4. Data Usage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "The app uses your scan data solely to provide disease detection and analysis services. No personal information is collected or transmitted.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "5. Data Security",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "We implement appropriate security measures to protect your data. However, no method of transmission over the internet is 100% secure.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "6. Your Rights",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "You have the right to access, modify, or delete your data at any time through the app settings. You can clear all data using the 'Clear All Data' option.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "7. Contact",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "For privacy concerns or questions, please contact us through the app's support channels.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { 
                Text("Close") 
            }
        }
    )
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val onClick: () -> Unit,
    val textColor: Color? = null,
    val hasNavigation: Boolean = true
)

data class SettingsItemWithToggle(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val isChecked: Boolean,
    val onToggle: (Boolean) -> Unit
)

@Composable
fun SettingsItemRowWithToggle(item: SettingsItemWithToggle) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.subtitle.isNotEmpty()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = item.isChecked,
            onCheckedChange = item.onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}