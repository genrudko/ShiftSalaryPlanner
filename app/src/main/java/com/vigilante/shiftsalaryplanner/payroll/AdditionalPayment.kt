package com.vigilante.shiftsalaryplanner.payroll

import java.util.UUID

enum class AdditionalPaymentType {
    MONTHLY,
    SALARY_PERCENT,
    HOURLY,
    ONE_TIME_MONTH,
    PREMIUM
}

enum class PremiumPeriod {
    MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    YEARLY
}

enum class PaymentDistribution {
    ADVANCE,
    SALARY,
    SPLIT_BY_HALF_MONTH
}

data class AdditionalPayment(
    val id: String = UUID.randomUUID().toString(),
    val workplaceId: String = "work_main",
    val name: String = "",
    val amount: Double = 0.0,
    val taxable: Boolean = true,
    val withAdvance: Boolean = false,
    val active: Boolean = true,
    val type: String = AdditionalPaymentType.MONTHLY.name,
    val premiumPeriod: String = PremiumPeriod.MONTHLY.name,
    val targetMonth: String = "",
    val delayMonths: Int = 0,
    val includeInShiftCost: Boolean = true,
    val distribution: String = if (withAdvance) {
        PaymentDistribution.ADVANCE.name
    } else {
        PaymentDistribution.SALARY.name
    }
) {
    fun resolvedType(): AdditionalPaymentType {
        return runCatching { AdditionalPaymentType.valueOf(type) }
            .getOrElse { AdditionalPaymentType.MONTHLY }
    }

    fun resolvedPremiumPeriod(): PremiumPeriod {
        return runCatching { PremiumPeriod.valueOf(premiumPeriod) }
            .getOrElse { PremiumPeriod.MONTHLY }
    }

    fun resolvedDistribution(): PaymentDistribution {
        return runCatching { PaymentDistribution.valueOf(distribution) }
            .getOrElse {
                if (withAdvance) PaymentDistribution.ADVANCE else PaymentDistribution.SALARY
            }
    }
}
