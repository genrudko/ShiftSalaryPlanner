package com.vigilante.shiftsalaryplanner

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.AppProfile
import com.vigilante.shiftsalaryplanner.settings.Workplace
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthHeader(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit
) {
    val context = LocalContext.current

    val ruLocale = remember { Locale.forLanguageTag("ru-RU") }

    val formatter = remember {
        DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale)
    }

    val monthTitle = currentMonth.atDay(1).format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(appPanelColor())
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(10.dp))
                .clickable(onClick = onPrevMonth),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "Предыдущий месяц",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clickable {
                    val initialDate = currentMonth.atDay(1)

                    DatePickerDialog(
                        context,
                        { _, year, month, _ ->
                            onPickMonth(YearMonth.of(year, month + 1))
                        },
                        initialDate.year,
                        initialDate.monthValue - 1,
                        initialDate.dayOfMonth
                    ).show()
                }
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(appPanelColor())
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(10.dp))
                .clickable(onClick = onNextMonth),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Следующий месяц",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CalendarProfileSwitcher(
    profiles: List<AppProfile>,
    activeProfileId: String,
    onSwitchProfile: (String) -> Unit,
    onOpenProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfileName = profiles.firstOrNull { it.id == activeProfileId }?.name
        ?: profiles.firstOrNull()?.name
        ?: "Профиль"
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { menuExpanded = true },
            shape = RoundedCornerShape(999.dp),
            color = appPanelColor()
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = activeProfileName,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Выбрать профиль",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            profiles.forEach { profile ->
                val isActive = profile.id == activeProfileId
                DropdownMenuItem(
                    text = {
                        Text(
                            text = profile.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        if (!isActive) {
                            onSwitchProfile(profile.id)
                        }
                    }
                )
            }

            DropdownMenuItem(
                text = { Text("Управление профилями") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null
                    )
                },
                onClick = {
                    menuExpanded = false
                    onOpenProfiles()
                }
            )
        }
    }
}

@Composable
fun CalendarWorkplaceSwitcher(
    workplaces: List<Workplace>,
    activeWorkplaceId: String,
    onSwitchWorkplace: (String) -> Unit,
    onOpenManageWorkplaces: (() -> Unit)? = null,
    showAllWorkplacesOption: Boolean = false,
    allWorkplacesOptionId: String = "__all_workplaces__",
    allWorkplacesOptionLabel: String = "Все работы",
    modifier: Modifier = Modifier
) {
    val activeWorkplaceName = when {
        showAllWorkplacesOption && activeWorkplaceId == allWorkplacesOptionId -> allWorkplacesOptionLabel
        else -> workplaces.firstOrNull { it.id == activeWorkplaceId }?.name
            ?: workplaces.firstOrNull()?.name
            ?: "Работа"
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { menuExpanded = true },
            shape = RoundedCornerShape(999.dp),
            color = appPanelColor()
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkHistory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = activeWorkplaceName,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Выбрать работу",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            if (showAllWorkplacesOption) {
                val isAllSelected = activeWorkplaceId == allWorkplacesOptionId
                DropdownMenuItem(
                    text = {
                        Text(
                            text = allWorkplacesOptionLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        if (isAllSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.WorkHistory,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        if (!isAllSelected) {
                            onSwitchWorkplace(allWorkplacesOptionId)
                        }
                    }
                )
            }

            workplaces.forEach { workplace ->
                val isActive = workplace.id == activeWorkplaceId
                DropdownMenuItem(
                    text = {
                        Text(
                            text = workplace.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.WorkHistory,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        if (!isActive) {
                            onSwitchWorkplace(workplace.id)
                        }
                    }
                )
            }

            if (onOpenManageWorkplaces != null) {
                DropdownMenuItem(
                    text = { Text("Переименовать работы") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenManageWorkplaces()
                    }
                )
            }
        }
    }
}
