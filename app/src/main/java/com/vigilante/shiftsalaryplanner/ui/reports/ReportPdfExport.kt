package com.vigilante.shiftsalaryplanner

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollQuantityUnit
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetSection
import java.io.ByteArrayOutputStream
import java.time.YearMonth

private const val PDF_PAGE_WIDTH = 595
private const val PDF_PAGE_HEIGHT = 842
private const val PDF_MARGIN = 36

fun buildPayrollSheetPdf(
    periodLabel: String,
    payrollDetailedResult: PayrollDetailedResult
): ByteArray {
    val summary = payrollDetailedResult.summary
    val lines = mutableListOf<String>()
    lines += "## Итоги"
    lines += "Начислено: ${formatMoney(summary.grossTotal)}"
    lines += "НДФЛ: ${formatMoney(summary.ndfl)}"
    lines += "На руки: ${formatMoney(summary.netTotal)}"
    lines += ""

    val sectionTitles = listOf(
        PayrollSheetSection.HEADER to "Общая информация",
        PayrollSheetSection.ACCRUAL to "Начислено за период",
        PayrollSheetSection.DEDUCTION to "Удержано за период",
        PayrollSheetSection.PRIOR_PAYMENT to "Аванс",
        PayrollSheetSection.PAYOUT to "К зарплате",
        PayrollSheetSection.REFERENCE to "Итоги периода"
    )

    sectionTitles.forEach { (section, title) ->
        val sectionItems = payrollDetailedResult.lineItems
            .filter { it.section == section }
            .sortedBy { it.sortOrder }
        if (sectionItems.isEmpty()) return@forEach

        lines += "## $title"
        sectionItems.forEach { item ->
            lines += formatPayrollPdfLine(item)
            if (!item.note.isNullOrBlank()) {
                lines += "  ${item.note}"
            }
        }
        lines += ""
    }

    return buildTextPdf(
        title = "Расчётный лист",
        subtitle = periodLabel,
        lines = lines
    )
}

fun buildMonthlyReportPdf(
    currentMonth: YearMonth,
    payrollSettings: PayrollSettings,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    additionalPayments: List<AdditionalPayment>,
    resolvedAdditionalPaymentsBreakdown: List<ResolvedAdditionalPaymentBreakdown>,
    detailedShiftStats: DetailedShiftStats
): ByteArray {
    val activePayments = additionalPayments.filter { it.active }
    val lines = mutableListOf<String>()

    lines += "## Период и режимы"
    lines += "Период: ${formatMonthYearTitle(currentMonth)}"
    lines += "Режим оплаты: ${payModeLabel(payrollSettings.payMode)}"
    lines += "Режим надбавки: ${extraSalaryModeLabel(payrollSettings.extraSalaryMode)}"
    lines += "Режим аванса: ${advanceModeLabel(payrollSettings.advanceMode)}"
    lines += "Режим нормы: ${normModeLabel(payrollSettings.normMode)}"
    lines += "Норма часов: ${formatDouble(payrollSettings.monthlyNormHours)}"
    lines += ""

    lines += "## Сводка по выплатам"
    lines += "Всего начислено: ${formatMoney(payroll.grossTotal)}"
    lines += "НДФЛ: ${formatMoney(payroll.ndfl)}"
    lines += "На руки: ${formatMoney(payroll.netTotal)}"
    lines += "Аванс: ${formatMoney(payroll.advanceAmount)} (${formatDate(paymentDates.advanceDate)})"
    lines += "К зарплате: ${formatMoney(payroll.salaryPaymentAmount)} (${formatDate(paymentDates.salaryDate)})"
    lines += ""

    lines += "## Статистика смен"
    lines += "Рабочих смен: ${detailedShiftStats.workedShiftCount}"
    lines += "Отработано часов: ${formatDouble(payroll.workedHours)}"
    lines += "Ночных часов: ${formatDouble(payroll.nightHours)}"
    lines += "Праздничных/выходных часов: ${formatDouble(payroll.holidayHours)}"
    lines += "Средняя стоимость смены (до НДФЛ): ${formatMoney(detailedShiftStats.shiftCostAverageGross)}"
    lines += "Средняя стоимость смены (на руки): ${formatMoney(detailedShiftStats.shiftCostAverageNet)}"
    lines += ""

    lines += "## Доплаты и удержания"
    lines += "${displayHousingPaymentLabel(housingPaymentLabel)}: ${formatMoney(payroll.housingPayment)}"
    lines += "Допвыплаты всего: ${formatMoney(payroll.additionalPaymentsTotal)}"
    lines += "Облагаемая база: ${formatMoney(payroll.taxableGrossTotal)}"
    lines += "Необлагаемые выплаты: ${formatMoney(payroll.nonTaxableTotal)}"
    lines += ""

    lines += "## Сверхурочка"
    lines += "Период: ${annualOvertime.periodLabel}"
    lines += "Статус: ${if (annualOvertime.enabled) "Включена" else "Отключена"}"
    lines += "Норма периода: ${formatDouble(annualOvertime.annualNormHours)}"
    lines += "Отработано: ${formatDouble(annualOvertime.workedHours)}"
    lines += "К оплате: ${formatDouble(annualOvertime.payableOvertimeHours)}"
    lines += "Доплата: ${formatMoney(annualOvertime.overtimePremiumAmount)}"
    lines += ""

    if (resolvedAdditionalPaymentsBreakdown.isNotEmpty()) {
        lines += "## Сработавшие доплаты и премии"
        resolvedAdditionalPaymentsBreakdown.forEach { breakdown ->
            lines += "${breakdown.payment.displayName}: ${formatMoney(breakdown.netAmount)} на руки"
        }
        lines += ""
    }

    if (activePayments.isNotEmpty()) {
        lines += "## Активные настройки начислений"
        activePayments.forEach { payment ->
            lines += "${payment.name.ifBlank { "Без названия" }}: ${additionalPaymentDetailsLabel(payment)}"
        }
        lines += ""
    }

    return buildTextPdf(
        title = "Подробный отчёт",
        subtitle = formatMonthYearTitle(currentMonth),
        lines = lines
    )
}

private fun formatPayrollPdfLine(item: PayrollLineItem): String {
    val isHeaderQuantity = item.section == PayrollSheetSection.HEADER && item.unit != PayrollQuantityUnit.NONE
    val normalizedQuantity = item.quantity ?: if (isHeaderQuantity) item.amount else null
    val quantityText = when (item.unit) {
        PayrollQuantityUnit.HOURS -> normalizedQuantity?.let { "${formatDouble(it)} ч" }
        PayrollQuantityUnit.DAYS -> normalizedQuantity?.let { "${formatDouble(it)} дн" }
        PayrollQuantityUnit.MONTHS -> normalizedQuantity?.let { "${formatDouble(it)} мес" }
        PayrollQuantityUnit.TIMES -> normalizedQuantity?.let { "${formatDouble(it)} раз" }
        PayrollQuantityUnit.NONE -> null
    }
    val amountText = if (isHeaderQuantity) {
        null
    } else {
        val prefix = if (item.section == PayrollSheetSection.DEDUCTION) "- " else ""
        "$prefix${formatMoney(item.amount)}"
    }

    val parts = listOfNotNull(
        item.title,
        quantityText,
        amountText
    )
    return "• ${parts.joinToString(" — ")}"
}

private fun buildTextPdf(
    title: String,
    subtitle: String,
    lines: List<String>
): ByteArray {
    val document = PdfDocument()
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        isFakeBoldText = true
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
    }
    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f
        isFakeBoldText = true
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
    }

    val left = PDF_MARGIN.toFloat()
    val right = (PDF_PAGE_WIDTH - PDF_MARGIN).toFloat()
    val contentWidth = right - left
    val bottom = (PDF_PAGE_HEIGHT - PDF_MARGIN).toFloat()

    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: android.graphics.Canvas? = null
    var y = 0f

    fun startNewPage() {
        page?.let(document::finishPage)
        pageNumber += 1
        page = document.startPage(
            PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pageNumber).create()
        )
        canvas = page?.canvas

        y = PDF_MARGIN.toFloat() + titlePaint.textSize
        canvas?.drawText(title, left, y, titlePaint)

        y += subtitlePaint.fontSpacing
        canvas?.drawText(subtitle, left, y, subtitlePaint)
        canvas?.drawText("Стр. $pageNumber", right - 44f, y, subtitlePaint)

        y += subtitlePaint.fontSpacing
    }

    fun ensureSpace(paint: Paint) {
        if (canvas == null || y + paint.fontSpacing > bottom) {
            startNewPage()
        }
    }

    startNewPage()

    lines.forEach { rawLine ->
        if (rawLine.isBlank()) {
            ensureSpace(bodyPaint)
            y += bodyPaint.fontSpacing * 0.55f
            return@forEach
        }

        val isSection = rawLine.startsWith("## ")
        val text = if (isSection) rawLine.removePrefix("## ").trim() else rawLine
        val paint = if (isSection) sectionPaint else bodyPaint

        wrapTextToWidth(text, paint, contentWidth).forEach { wrappedLine ->
            ensureSpace(paint)
            canvas?.drawText(wrappedLine, left, y, paint)
            y += paint.fontSpacing
        }

        if (isSection) {
            ensureSpace(bodyPaint)
            y += bodyPaint.fontSpacing * 0.2f
        }
    }

    page?.let(document::finishPage)
    val output = ByteArrayOutputStream()
    document.writeTo(output)
    document.close()
    return output.toByteArray()
}

private fun wrapTextToWidth(
    text: String,
    paint: Paint,
    maxWidth: Float
): List<String> {
    if (text.isBlank()) return listOf("")
    val result = mutableListOf<String>()
    var remaining = text.trim()

    while (remaining.isNotEmpty()) {
        val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
        if (count >= remaining.length) {
            result += remaining
            break
        }

        var split = remaining.lastIndexOf(' ', count)
        if (split <= 0) split = count
        val line = remaining.substring(0, split).trim()
        if (line.isNotEmpty()) {
            result += line
        }
        remaining = remaining.substring(split).trimStart()
    }

    return if (result.isEmpty()) listOf(text) else result
}
