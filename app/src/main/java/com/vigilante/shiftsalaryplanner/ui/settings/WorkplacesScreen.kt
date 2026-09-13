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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.Workplace

@Composable
fun WorkplacesScreen(
    workplaces: List<Workplace>,
    activeWorkplaceId: String,
    onBack: () -> Unit,
    onSelectWorkplace: (String) -> Unit,
    onRenameWorkplaces: () -> Unit,
    onOpenPayrollSettings: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = appHapticAction(onAction = onBack)) { Text("Назад") }
            Text("Рабочие места", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = appHapticAction(onAction = onRenameWorkplaces)) { Text("Названия") }
        }
        Text("Каждая работа хранит свой контекст смен и расчёта. Активную работу можно переключить здесь.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        workplaces.forEachIndexed { index, workplace ->
            val active = workplace.id == activeWorkplaceId
            EvolutionSurface(
                modifier = Modifier.fillMaxWidth(),
                role = if (active) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
                shape = RoundedCornerShape(appCornerRadius(20.dp)),
                shadowElevation = 0.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(appCardPadding()), verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                    Text(workplace.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (active) "Активная работа" else "Работа ${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (!active) {
                            TextButton(onClick = appHapticAction { onSelectWorkplace(workplace.id) }) { Text("Выбрать") }
                        }
                        TextButton(onClick = appHapticAction { onOpenPayrollSettings(workplace.id) }) { Text("Расчёт") }
                    }
                }
            }
        }
        Spacer(Modifier.height(appScaledSpacing(24.dp)))
    }
}
