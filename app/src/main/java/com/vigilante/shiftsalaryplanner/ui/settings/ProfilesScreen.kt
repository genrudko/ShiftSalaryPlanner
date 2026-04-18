package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.AppProfileStore
import com.vigilante.shiftsalaryplanner.settings.AppProfilesState

@Composable
fun ProfilesScreen(
    state: AppProfilesState,
    onBack: () -> Unit,
    onActivateProfile: (String) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    var newProfileName by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(
                title = "Профили",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(appScreenPadding()),
                verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                SettingsSectionCard(
                    title = "Новый профиль",
                    subtitle = "Каждый профиль хранит собственные смены, расчёты, будильники и настройки",
                    content = {
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Имя профиля") }
                        )
                        Spacer(modifier = Modifier.height(appScaledSpacing(8.dp)))
                        TextButton(
                            onClick = {
                                onCreateProfile(newProfileName)
                                newProfileName = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Создать и переключиться")
                        }
                    }
                )

                SettingsSectionCard(
                    title = "Список профилей",
                    subtitle = "Активный профиль применяется сразу после переключения",
                    content = {
                        state.profiles.forEachIndexed { index, profile ->
                            ProfileRow(
                                profileName = profile.name,
                                isActive = profile.id == state.activeProfileId,
                                canDelete = profile.id != AppProfileStore.DEFAULT_PROFILE_ID,
                                onActivate = { onActivateProfile(profile.id) },
                                onRename = { onRenameProfile(profile.id, it) },
                                onDelete = { onDeleteProfile(profile.id) }
                            )
                            if (index != state.profiles.lastIndex) {
                                Spacer(modifier = Modifier.height(appScaledSpacing(8.dp)))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(appScaledSpacing(80.dp)))
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profileName: String,
    isActive: Boolean,
    canDelete: Boolean,
    onActivate: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editingName by remember(profileName) { mutableStateOf(profileName) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(appCornerRadius(16.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = if (isActive) 0.34f else 0.22f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = profileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    Text(
                        text = "Активный",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(8.dp)))
            OutlinedTextField(
                value = editingName,
                onValueChange = { editingName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Имя") }
            )

            Spacer(modifier = Modifier.height(appScaledSpacing(8.dp)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onActivate) {
                    Text("Сделать активным")
                }
                TextButton(onClick = { onRename(editingName) }) {
                    Text("Переименовать")
                }
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить")
                    }
                }
            }
        }
    }
}

