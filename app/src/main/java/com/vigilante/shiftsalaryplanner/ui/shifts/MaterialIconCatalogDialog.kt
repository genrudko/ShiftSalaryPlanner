package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MaterialIconCatalogDialog(
    currentIconKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    val allKeys = remember(context) { materialRoundedIconKeys(context) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf(MATERIAL_CATEGORY_ALL_ID) }

    val categoryByKey = remember(allKeys) {
        allKeys.associateWith { key -> materialIconCategoryIdForKey(key) }
    }

    val queryFilteredKeys = remember(allKeys, searchQuery) {
        if (searchQuery.isBlank()) {
            allKeys
        } else {
            allKeys.filter { matchesShiftIconQuery(it, searchQuery) }
        }
    }

    val categoryCounts = remember(queryFilteredKeys, categoryByKey) {
        buildMap {
            put(MATERIAL_CATEGORY_ALL_ID, queryFilteredKeys.size)
            MATERIAL_ICON_CATEGORIES
                .asSequence()
                .filterNot { it.id == MATERIAL_CATEGORY_ALL_ID }
                .forEach { category ->
                    put(category.id, queryFilteredKeys.count { key -> categoryByKey[key] == category.id })
                }
        }
    }

    val filteredKeys = remember(queryFilteredKeys, selectedCategoryId, categoryByKey) {
        if (selectedCategoryId == MATERIAL_CATEGORY_ALL_ID) {
            queryFilteredKeys
        } else {
            queryFilteredKeys.filter { key -> categoryByKey[key] == selectedCategoryId }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(10.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Каталог Material Icons",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (searchQuery.isBlank() && selectedCategoryId == MATERIAL_CATEGORY_ALL_ID) {
                                "Доступно: ${allKeys.size}"
                            } else {
                                "Найдено: ${filteredKeys.size} из ${allKeys.size}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = appListSecondaryTextColor()
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, appPanelBorderColor())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = appListSecondaryTextColor()
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.take(50) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.merge(
                                TextStyle(color = MaterialTheme.colorScheme.onSurface)
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isBlank()) {
                                    Text(
                                        text = "Поиск: work, timer, calendar, car, ночь, выплата…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appListSecondaryTextColor()
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MATERIAL_ICON_CATEGORIES, key = { it.id }) { category ->
                        val resultCount = categoryCounts[category.id] ?: 0
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = {
                                Text(
                                    text = "${category.title} ($resultCount)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredKeys.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.bodySmall,
                            color = appListSecondaryTextColor()
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 90.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredKeys, key = { it }) { key ->
                            MaterialIconGridTile(
                                iconKey = key,
                                selected = key == currentIconKey,
                                onClick = { onSelect(key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MATERIAL_CATEGORY_ALL_ID = "all"
private const val MATERIAL_CATEGORY_OTHER_ID = "other"

private data class MaterialIconCategory(
    val id: String,
    val title: String,
    val keywords: Set<String> = emptySet()
)

private val MATERIAL_ICON_CATEGORIES = listOf(
    MaterialIconCategory(MATERIAL_CATEGORY_ALL_ID, "Все"),
    MaterialIconCategory(
        id = "work",
        title = "Работа",
        keywords = setOf(
            "work", "business", "task", "assignment", "engineering", "construction",
            "factory", "store", "shopping", "inventory", "delivery", "shipping", "badge"
        )
    ),
    MaterialIconCategory(
        id = "time",
        title = "Время",
        keywords = setOf(
            "time", "timer", "alarm", "clock", "watch", "schedule", "calendar",
            "event", "today", "history", "date", "hour", "bedtime", "night", "sun"
        )
    ),
    MaterialIconCategory(
        id = "transport",
        title = "Транспорт",
        keywords = setOf(
            "car", "bus", "train", "flight", "air", "bike", "motor", "scooter",
            "tram", "subway", "rail", "taxi", "directions", "navigation", "route"
        )
    ),
    MaterialIconCategory(
        id = "finance",
        title = "Финансы",
        keywords = setOf(
            "money", "payment", "pay", "wallet", "bank", "cash", "coin", "card",
            "credit", "debit", "receipt", "savings", "attachmoney", "payments", "balance"
        )
    ),
    MaterialIconCategory(
        id = "medical",
        title = "Медицина",
        keywords = setOf(
            "medical", "health", "hospital", "healing", "pharmacy", "fitness",
            "heart", "emergency", "vaccines", "blood", "monitor", "medicine"
        )
    ),
    MaterialIconCategory(
        id = "docs",
        title = "Документы",
        keywords = setOf(
            "book", "article", "description", "document", "folder", "file", "note",
            "notes", "receipt", "library", "list", "menu"
        )
    ),
    MaterialIconCategory(
        id = "communication",
        title = "Связь",
        keywords = setOf(
            "phone", "call", "message", "chat", "email", "mail", "sms", "forum",
            "contact", "notification", "wifi", "bluetooth", "link", "share"
        )
    ),
    MaterialIconCategory(
        id = "system",
        title = "Система",
        keywords = setOf(
            "settings", "tune", "dashboard", "widget", "security", "shield", "lock",
            "key", "sync", "autorenew", "backup", "cloud", "power", "battery", "admin", "code"
        )
    ),
    MaterialIconCategory(MATERIAL_CATEGORY_OTHER_ID, "Прочее")
)

private fun materialIconCategoryIdForKey(iconKey: String): String {
    val searchable = shiftIconSearchTokens(iconKey).joinToString(" ").lowercase()
    return MATERIAL_ICON_CATEGORIES.firstOrNull { category ->
        category.id != MATERIAL_CATEGORY_ALL_ID &&
            category.id != MATERIAL_CATEGORY_OTHER_ID &&
            category.keywords.any { keyword -> searchable.contains(keyword) }
    }?.id ?: MATERIAL_CATEGORY_OTHER_ID
}

@Composable
private fun MaterialIconGridTile(
    iconKey: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val title = shiftIconLabel(iconKey).removePrefix("Material • ").trim()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else appPanelBorderColor()
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(
                iconKey = iconKey,
                fallbackCode = "?",
                badgeColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
                size = 30.dp,
                shape = RoundedCornerShape(9.dp),
                selected = selected,
                unselectedBorderColor = appPanelBorderColor()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
