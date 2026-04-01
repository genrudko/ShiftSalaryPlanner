package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment

@Composable
fun AdditionalPaymentsManagementScreen(
    payments: List<AdditionalPayment>,
    onBack: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (AdditionalPayment) -> Unit,
    onDeletePayment: (AdditionalPayment) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FixedScreenHeader(
                title = "Доплаты, выплаты и премии",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                AdditionalPaymentsCard(
                    payments = payments,
                    onAddPayment = onAddPayment,
                    onEditPayment = onEditPayment,
                    onDeletePayment = onDeletePayment
                )
            }
        }
    }
}
