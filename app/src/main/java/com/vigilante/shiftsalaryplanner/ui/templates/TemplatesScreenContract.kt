package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.settings.Workplace

data class TemplatesScreenState(
    val mode: TemplateMode,
    val templates: List<ShiftTemplateEntity>,
    val systemStatusCodes: Set<String>,
    val specialRules: Map<String, ShiftSpecialRule>,
    val patterns: List<PatternTemplate>,
    val workplaces: List<Workplace>,
    val activeWorkplaceId: String
)

data class TemplatesScreenActions(
    val onModeChange: (TemplateMode) -> Unit,
    val onBack: () -> Unit,
    val onSwitchWorkplace: (String) -> Unit,
    val onOpenManageWorkplaces: () -> Unit,
    val onAddShift: () -> Unit,
    val onAddSystemStatus: () -> Unit,
    val onEditShift: (ShiftTemplateEntity) -> Unit,
    val onDuplicateShift: (ShiftTemplateEntity) -> Unit,
    val onDeleteShift: (ShiftTemplateEntity) -> Unit,
    val onAddPattern: () -> Unit,
    val onEditPattern: (PatternTemplate) -> Unit,
    val onApplyPattern: (PatternTemplate) -> Unit,
    val onDeletePattern: (PatternTemplate) -> Unit
)
