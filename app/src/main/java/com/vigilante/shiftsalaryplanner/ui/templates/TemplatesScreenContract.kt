package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate

data class TemplatesScreenState(
    val mode: TemplateMode,
    val templates: List<ShiftTemplateEntity>,
    val specialRules: Map<String, ShiftSpecialRule>,
    val patterns: List<PatternTemplate>
)

data class TemplatesScreenActions(
    val onModeChange: (TemplateMode) -> Unit,
    val onBack: () -> Unit,
    val onAddShift: () -> Unit,
    val onEditShift: (ShiftTemplateEntity) -> Unit,
    val onDuplicateShift: (ShiftTemplateEntity) -> Unit,
    val onDeleteShift: (ShiftTemplateEntity) -> Unit,
    val onAddPattern: () -> Unit,
    val onEditPattern: (PatternTemplate) -> Unit,
    val onApplyPattern: (PatternTemplate) -> Unit,
    val onDeletePattern: (PatternTemplate) -> Unit
)
