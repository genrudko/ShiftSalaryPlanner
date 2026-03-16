package com.vigilante.shiftsalaryplanner.payroll

import java.util.UUID

data class AdditionalPayment(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amount: Double = 0.0,
    val taxable: Boolean = true,
    val withAdvance: Boolean = false,
    val active: Boolean = true
)