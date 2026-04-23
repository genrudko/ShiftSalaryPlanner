package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                        CompactTextField(
                            label = "Имя профиля",
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(appBlockSpacing()))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            ProfileActionPill(
                                text = "Создать",
                                icon = Icons.Rounded.Add,
                                emphasized = true,
                                onClick = {
                                    onCreateProfile(newProfileName)
                                    newProfileName = ""
                                }
                            )
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
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = if (isActive) 0.34f else 0.22f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appScaledSpacing(9.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = profileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.26f))
                    ) {
                        Text(
                            text = "Активный",
                            modifier = Modifier.padding(
                                horizontal = appScaledSpacing(8.dp),
                                vertical = appScaledSpacing(3.dp)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
            CompactTextField(
                label = "Имя",
                value = editingName,
                onValueChange = { editingName = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(appScaledSpacing(7.dp)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
            ) {
                ProfileActionPill(
                    text = if (isActive) "Выбран" else "Активировать",
                    icon = Icons.Rounded.CheckCircle,
                    onClick = onActivate,
                    emphasized = isActive,
                    enabled = !isActive,
                    modifier = Modifier.weight(1f)
                )
                ProfileActionPill(
                    text = "Сохранить",
                    icon = Icons.Rounded.Edit,
                    onClick = { onRename(editingName) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (canDelete) {
                Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ProfileActionPill(
                        text = "Удалить",
                        icon = Icons.Rounded.Delete,
                        onClick = onDelete,
                        destructive = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileActionPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val containerColor = when {
        destructive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (enabled) 0.32f else 0.18f)
        emphasized -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.86f else 0.30f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.42f else 0.20f)
    }
    val contentColor = when {
        !enabled -> appListSecondaryTextColor(alpha = 0.72f)
        destructive -> MaterialTheme.colorScheme.error
        emphasized -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        destructive -> MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 0.35f else 0.16f)
        emphasized -> MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.34f else 0.16f)
        else -> appPanelBorderColor().copy(alpha = if (enabled) 0.76f else 0.42f)
    }

    Surface(
        modifier = modifier
            .height(appLargeButtonHeight(36.dp))
            .clickable(enabled = enabled, onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(10.dp)),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = appScaledSpacing(10.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(appScaledSpacing(14.dp)),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(appScaledSpacing(4.dp)))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}
