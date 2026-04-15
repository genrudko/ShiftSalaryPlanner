package com.vigilante.shiftsalaryplanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AlimonySharePreset
import com.vigilante.shiftsalaryplanner.payroll.DeductionBasisDocumentType
import com.vigilante.shiftsalaryplanner.payroll.DeductionLegalKind
import com.vigilante.shiftsalaryplanner.payroll.DeductionMode
import com.vigilante.shiftsalaryplanner.payroll.DeductionType
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import com.vigilante.shiftsalaryplanner.payroll.defaultLegacyPriority
import com.vigilante.shiftsalaryplanner.payroll.defaultLimitPercent
import com.vigilante.shiftsalaryplanner.payroll.defaultQueue
import com.vigilante.shiftsalaryplanner.payroll.displayName
import com.vigilante.shiftsalaryplanner.payroll.inferLegalKindFromType
import com.vigilante.shiftsalaryplanner.payroll.legalKindOptions
import com.vigilante.shiftsalaryplanner.payroll.resolvedType
import java.util.UUID

@Composable
fun DeductionEditorScreen(
    currentDeduction: PayrollDeduction?,
    onBack: () -> Unit,
    onSave: (PayrollDeduction) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(currentDeduction?.title ?: "") }
    var typeName by rememberSaveable { mutableStateOf(currentDeduction?.type ?: DeductionType.OTHER.name) }
    var modeName by rememberSaveable { mutableStateOf(currentDeduction?.mode ?: DeductionMode.FIXED.name) }
    var valueText by rememberSaveable {
        mutableStateOf(
            currentDeduction?.value?.let { value ->
                if (kotlin.math.abs(value - value.toInt()) < 0.0001) value.toInt().toString()
                else value.toString().replace('.', ',')
            } ?: ""
        )
    }
    var shareLabel by rememberSaveable { mutableStateOf(currentDeduction?.shareLabel ?: "") }

    var legalKindName by rememberSaveable {
        mutableStateOf(
            currentDeduction?.legalKind
                ?: inferLegalKindFromType(currentDeduction?.resolvedType() ?: DeductionType.OTHER).name
        )
    }
    var basisDocumentTypeName by rememberSaveable {
        mutableStateOf(currentDeduction?.basisDocumentType ?: DeductionBasisDocumentType.OTHER.name)
    }
    var recipientName by rememberSaveable { mutableStateOf(currentDeduction?.recipientName ?: "") }
    var caseNumber by rememberSaveable { mutableStateOf(currentDeduction?.caseNumber ?: "") }
    var fixedAmountIndexed by rememberSaveable { mutableStateOf(currentDeduction?.fixedAmountIndexed ?: false) }
    var preserveMinimumIncome by rememberSaveable { mutableStateOf(currentDeduction?.preserveMinimumIncome ?: false) }

    var applyToAdvance by rememberSaveable { mutableStateOf(currentDeduction?.applyToAdvance ?: false) }
    var applyToSalary by rememberSaveable { mutableStateOf(currentDeduction?.applyToSalary ?: true) }
    var active by rememberSaveable { mutableStateOf(currentDeduction?.active ?: true) }
    var note by rememberSaveable { mutableStateOf(currentDeduction?.note ?: "") }

    var showExitDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val hasChanges = remember(
        title, typeName, modeName, valueText, shareLabel,
        legalKindName, basisDocumentTypeName, recipientName, caseNumber,
        fixedAmountIndexed, preserveMinimumIncome,
        applyToAdvance, applyToSalary, active, note, currentDeduction
    ) {
        if (currentDeduction == null) {
            title.isNotBlank() ||
                    valueText.isNotBlank() ||
                    shareLabel.isNotBlank() ||
                    legalKindName != inferLegalKindFromType(DeductionType.OTHER).name ||
                    basisDocumentTypeName != DeductionBasisDocumentType.OTHER.name ||
                    recipientName.isNotBlank() ||
                    caseNumber.isNotBlank() ||
                    fixedAmountIndexed ||
                    preserveMinimumIncome ||
                    applyToAdvance ||
                    !applyToSalary ||
                    !active ||
                    note.isNotBlank() ||
                    typeName != DeductionType.OTHER.name ||
                    modeName != DeductionMode.FIXED.name
        } else {
            title != currentDeduction.title ||
                    typeName != currentDeduction.type ||
                    modeName != currentDeduction.mode ||
                    valueText.normalizeDecimalText() != currentDeduction.value.toString().normalizeDecimalText() ||
                    shareLabel != currentDeduction.shareLabel ||
                    legalKindName != currentDeduction.legalKind ||
                    basisDocumentTypeName != currentDeduction.basisDocumentType ||
                    recipientName != currentDeduction.recipientName ||
                    caseNumber != currentDeduction.caseNumber ||
                    fixedAmountIndexed != currentDeduction.fixedAmountIndexed ||
                    preserveMinimumIncome != currentDeduction.preserveMinimumIncome ||
                    applyToAdvance != currentDeduction.applyToAdvance ||
                    applyToSalary != currentDeduction.applyToSalary ||
                    active != currentDeduction.active ||
                    note != currentDeduction.note
        }
    }

    fun handleBack() {
        if (hasChanges) showExitDialog = true else onBack()
    }

    fun buildDeductionOrNull(): PayrollDeduction? {
        val parsedValue = valueText.replace(',', '.').toDoubleOrNull()
        if (title.isBlank()) {
            validationMessage = "Укажи название удержания"
            return null
        }
        if (parsedValue == null || parsedValue < 0.0) {
            validationMessage = "Укажи корректное значение"
            return null
        }
        if (!applyToAdvance && !applyToSalary) {
            validationMessage = "Выбери, удерживать из аванса и/или зарплаты"
            return null
        }

        val resolvedType = runCatching { DeductionType.valueOf(typeName) }
            .getOrElse { DeductionType.OTHER }

        val legalKind = runCatching { DeductionLegalKind.valueOf(legalKindName) }
            .getOrElse { inferLegalKindFromType(resolvedType) }

        val basisDocumentType = runCatching { DeductionBasisDocumentType.valueOf(basisDocumentTypeName) }
            .getOrElse { DeductionBasisDocumentType.OTHER }

        return PayrollDeduction(
            id = currentDeduction?.id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            type = typeName,
            mode = modeName,
            value = parsedValue,
            active = active,
            applyToAdvance = applyToAdvance,
            applyToSalary = applyToSalary,

            legalKind = legalKind.name,
            basisDocumentType = basisDocumentType.name,
            recipientName = recipientName.trim(),
            caseNumber = caseNumber.trim(),
            fixedAmountIndexed = fixedAmountIndexed,
            preserveMinimumIncome = preserveMinimumIncome,

            note = note.trim(),
            shareLabel = shareLabel.trim(),

            priority = legalKind.defaultLegacyPriority(),
            maxPercentLimit = legalKind.defaultLimitPercent()
        )
    }

    fun applyTypeSelection(type: DeductionType) {
        typeName = type.name
        val allowedKinds = legalKindOptions(type)
        if (allowedKinds.none { it.name == legalKindName }) {
            legalKindName = allowedKinds.first().name
        }
        if (type != DeductionType.ALIMONY) {
            shareLabel = ""
            fixedAmountIndexed = false
        }
    }

    BackHandler(onBack = ::handleBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appInnerSurfaceColor())
    ) {
        FixedScreenHeader(
            title = if (currentDeduction == null) "Новое удержание" else "Редактирование удержания",
            onBack = ::handleBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            validationMessage?.let { message ->
                InfoCard(title = "Ошибка") {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            InfoCard(title = "Основное") {
                CompactTextField(
                    label = "Название",
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Тип удержания",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(6.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "Алименты",
                            subtitle = "Доля или фикс",
                            selected = typeName == DeductionType.ALIMONY.name,
                            onClick = { applyTypeSelection(DeductionType.ALIMONY) },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                        PayModeChoiceCard(
                            title = "Исп. пр-во",
                            subtitle = "Исполнительные листы",
                            selected = typeName == DeductionType.ENFORCEMENT.name,
                            onClick = { applyTypeSelection(DeductionType.ENFORCEMENT) },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }
                    PayModeChoiceCard(
                        title = "Прочее",
                        subtitle = "Прочие удержания",
                        selected = typeName == DeductionType.OTHER.name,
                        onClick = { applyTypeSelection(DeductionType.OTHER) },
                        showSubtitle = false
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (typeName) {
                        DeductionType.ALIMONY.name -> "Удержание по алиментным обязательствам"
                        DeductionType.ENFORCEMENT.name -> "Удержание по исполнительному документу"
                        else -> "Прочее удержание после НДФЛ"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(title = "Способ расчёта") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "Доля",
                            subtitle = "1/4, 1/3 и т.д.",
                            selected = modeName == DeductionMode.SHARE.name,
                            onClick = {
                                modeName = DeductionMode.SHARE.name
                            },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                        PayModeChoiceCard(
                            title = "Процент",
                            subtitle = "От базы удержаний",
                            selected = modeName == DeductionMode.PERCENT.name,
                            onClick = {
                                modeName = DeductionMode.PERCENT.name
                                shareLabel = ""
                            },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                        PayModeChoiceCard(
                            title = "Сумма",
                            subtitle = "Фиксированная",
                            selected = modeName == DeductionMode.FIXED.name,
                            onClick = {
                                modeName = DeductionMode.FIXED.name
                                shareLabel = ""
                            },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (modeName == DeductionMode.SHARE.name) {
                    Text(
                        text = "Пресеты для алиментов",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SharePresetRow(
                        title = "1/4 — один ребёнок",
                        selected = shareLabel == AlimonySharePreset.ONE_CHILD.label,
                        onClick = {
                            shareLabel = AlimonySharePreset.ONE_CHILD.label
                            valueText = "0,25"
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SharePresetRow(
                        title = "1/3 — двое детей",
                        selected = shareLabel == AlimonySharePreset.TWO_CHILDREN.label,
                        onClick = {
                            shareLabel = AlimonySharePreset.TWO_CHILDREN.label
                            valueText = "0,3333"
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SharePresetRow(
                        title = "1/2 — трое и более",
                        selected = shareLabel == AlimonySharePreset.THREE_PLUS.label,
                        onClick = {
                            shareLabel = AlimonySharePreset.THREE_PLUS.label
                            valueText = "0,5"
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CompactTextField(
                        label = "Подпись доли",
                        value = shareLabel,
                        onValueChange = { shareLabel = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CompactDecimalField(
                        label = "Значение доли (например 0,25)",
                        value = valueText,
                        onValueChange = { valueText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CompactDecimalField(
                        label = when (modeName) {
                            DeductionMode.PERCENT.name -> "Процент"
                            else -> "Сумма"
                        },
                        value = valueText,
                        onValueChange = { valueText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            InfoCard(title = "Правовая квалификация") {
                val currentType = runCatching { DeductionType.valueOf(typeName) }
                    .getOrElse { DeductionType.OTHER }
                val availableKinds = legalKindOptions(currentType)
                val currentKind = runCatching { DeductionLegalKind.valueOf(legalKindName) }
                    .getOrElse { inferLegalKindFromType(currentType) }

                Text(
                    text = "Категория взыскания",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                availableKinds.forEach { kind ->
                    SelectableRow(
                        selected = legalKindName == kind.name,
                        title = kind.displayName(),
                        onClick = { legalKindName = kind.name }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Основание",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                DeductionBasisDocumentType.entries.forEach { docType ->
                    SelectableRow(
                        selected = basisDocumentTypeName == docType.name,
                        title = docType.displayName(),
                        onClick = { basisDocumentTypeName = docType.name }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                CompactTextField(
                    label = "Получатель / взыскатель",
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                CompactTextField(
                    label = "Номер документа / производства",
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (typeName == DeductionType.ALIMONY.name && modeName == DeductionMode.FIXED.name) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniToggleChip(
                            title = "Индексировать",
                            checked = fixedAmountIndexed,
                            onCheckedChange = { fixedAmountIndexed = it },
                            modifier = Modifier.weight(1f)
                        )
                        MiniToggleChip(
                            title = "Сохр. минимум",
                            checked = preserveMinimumIncome,
                            onCheckedChange = { preserveMinimumIncome = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    MiniToggleChip(
                        title = "Сохранять прожиточный минимум",
                        checked = preserveMinimumIncome,
                        onCheckedChange = { preserveMinimumIncome = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Очередь: ${currentKind.defaultQueue().displayName()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Лимит удержаний: ${formatSimplePercent(currentKind.defaultLimitPercent())}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            InfoCard(title = "Применение") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniToggleChip(
                        title = "Аванс",
                        checked = applyToAdvance,
                        onCheckedChange = { applyToAdvance = it },
                        modifier = Modifier.weight(1f)
                    )
                    MiniToggleChip(
                        title = "Зарплата",
                        checked = applyToSalary,
                        onCheckedChange = { applyToSalary = it },
                        modifier = Modifier.weight(1f)
                    )
                    MiniToggleChip(
                        title = "Активно",
                        checked = active,
                        onCheckedChange = { active = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                CompactTextField(
                    label = "Примечание",
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    validationMessage = null
                    buildDeductionOrNull()?.let(onSave)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить")
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Сохранить изменения?") },
            text = { Text("Есть несохранённые изменения.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        buildDeductionOrNull()?.let {
                            onSave(it)
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { }) {
                        Text("Отмена")
                    }
                    TextButton(
                        onClick = {
                            onBack()
                        }
                    ) {
                        Text("Не сохранять")
                    }
                }
            }
        )
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        appPanelBorderColor()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(appPanelColor(), RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        if (selected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text("✓", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SharePresetRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectableRow(
        selected = selected,
        title = title,
        onClick = onClick
    )
}

@Composable
private fun MiniToggleChip(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.56f)
                )
            }
        }
    }
}

private fun String.normalizeDecimalText(): String =
    replace(',', '.').trim()
private fun formatSimplePercent(value: Double): String {
    val normalized = if (kotlin.math.abs(value - value.toInt()) < 0.0001) {
        value.toInt().toString()
    } else {
        value.toString().replace('.', ',')
    }
    return "$normalized%"
}
