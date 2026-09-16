package com.vigilante.shiftsalaryplanner.wearsync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.vigilante.shiftsalaryplanner.ShiftAlarmConfig
import com.vigilante.shiftsalaryplanner.ShiftAlarmPlaybackService
import com.vigilante.shiftsalaryplanner.ShiftAlarmSettings
import com.vigilante.shiftsalaryplanner.ShiftAlarmUpcomingInfo
import com.vigilante.shiftsalaryplanner.ShiftAlarmVibrationType
import com.vigilante.shiftsalaryplanner.ShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.calculateMonthlyPaymentMultiplierForDateRange
import com.vigilante.shiftsalaryplanner.defaultShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.formatClockHm
import com.vigilante.shiftsalaryplanner.paidHours
import com.vigilante.shiftsalaryplanner.payroll.calculatePaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollCalculator
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.WorkShiftItem
import com.vigilante.shiftsalaryplanner.rescheduleShiftAlarms
import com.vigilante.shiftsalaryplanner.app.ports.DefaultAlarmPlatformPort
import com.vigilante.shiftsalaryplanner.resolveAdditionalPaymentsForPeriod
import com.vigilante.shiftsalaryplanner.settings.AdditionalPaymentsStore
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNotesStore
import com.vigilante.shiftsalaryplanner.settings.DeductionsStore
import com.vigilante.shiftsalaryplanner.settings.PayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsState
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsStore
import com.vigilante.shiftsalaryplanner.settings.Workplace
import com.vigilante.shiftsalaryplanner.settings.defaultWorkplaces
import com.vigilante.shiftsalaryplanner.stripWorkplaceScopeFromShiftCode
import com.vigilante.shiftsalaryplanner.toWorkShiftItemForDate
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.feature.notes.compat.createNotesV2Repository
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

object WearSyncBridge {

    private val asyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun publishSnapshotAsync(context: Context) {
        val appContext = context.applicationContext
        asyncScope.launch {
            runCatching { publishSnapshot(appContext) }
        }
    }

    fun publishAlarmRingAsync(
        context: Context,
        alarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String,
        snoozeIntervalMinutes: Int,
        snoozeCountLimit: Int,
        snoozeCurrentCount: Int,
        ringDurationSeconds: Int,
        rampUpDurationSeconds: Int,
        vibrationEnabled: Boolean,
        vibrationType: ShiftAlarmVibrationType,
        vibrationDurationSeconds: Int,
        customVibrationPattern: String
    ) {
        val appContext = context.applicationContext
        asyncScope.launch {
            runCatching {
                val settings = ShiftAlarmStore(appContext).current()
                if (!settings.wearMirrorEnabled) return@runCatching
                sendMessageToWear(
                    context = appContext,
                    path = WearSyncContract.PATH_ALARM_RING,
                    payload = JSONObject()
                        .put("alarmKey", alarmKey)
                        .put("title", title)
                        .put("text", text)
                        .put("startedAt", System.currentTimeMillis())
                        .put("volumePercent", volumePercent.coerceIn(0, 100))
                        .put("soundUri", soundUri.orEmpty())
                        .put("soundLabel", soundLabel)
                        .put("wearSoundMode", settings.wearSoundMode.name)
                        .put("snoozeIntervalMinutes", snoozeIntervalMinutes.coerceIn(1, 120))
                        .put("snoozeCountLimit", snoozeCountLimit.coerceIn(0, 10))
                        .put("snoozeCurrentCount", snoozeCurrentCount.coerceAtLeast(0))
                        .put("ringDurationSeconds", ringDurationSeconds.coerceIn(10, 3_600))
                        .put("rampUpDurationSeconds", rampUpDurationSeconds.coerceIn(0, 180))
                        .put("vibrationEnabled", vibrationEnabled)
                        .put("vibrationType", vibrationType.name)
                        .put("vibrationDurationSeconds", vibrationDurationSeconds.coerceIn(0, 300))
                        .put("customVibrationPattern", customVibrationPattern.trim())
                )
            }
        }
    }

    fun publishAlarmStopAsync(context: Context, alarmKey: String, snoozed: Boolean = false) {
        val appContext = context.applicationContext
        asyncScope.launch {
            runCatching {
                sendMessageToWear(
                    context = appContext,
                    path = if (snoozed) WearSyncContract.PATH_ALARM_SNOOZE else WearSyncContract.PATH_ALARM_STOP,
                    payload = JSONObject()
                        .put("alarmKey", alarmKey)
                        .put("stoppedAt", System.currentTimeMillis())
                )
            }
        }
    }

    suspend fun publishSnapshot(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val data = loadData(appContext)
        val snapshotJson = buildSnapshotJson(data).toString()
        val request = PutDataMapRequest.create(WearSyncContract.SNAPSHOT_PATH).apply {
            dataMap.putString(WearSyncContract.KEY_SNAPSHOT_JSON, snapshotJson)
            dataMap.putLong(WearSyncContract.KEY_GENERATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(appContext).putDataItem(request).await()
    }

    suspend fun addNoteFromWear(context: Context, payload: ByteArray) {
        val appContext = context.applicationContext
        val body = payloadJson(payload).optString("body").trim()
        if (body.isBlank()) {
            publishSnapshot(appContext)
            return
        }
        val requestedDate = payloadJson(payload).optString("date")
        val date = runCatching { LocalDate.parse(requestedDate) }
            .getOrDefault(LocalDate.now())
        val saved = createNotesV2Repository(appContext).saveLegacy(
            AppNote(
                date = date.toString(),
                title = body.lineSequence().firstOrNull().orEmpty().take(42),
                body = body,
                colorHex = "#DFF8E8"
            )
        )
        if (!saved) {
            publishSnapshot(appContext)
            return
        }
        publishSnapshot(appContext)
    }

    suspend fun setAllAlarmsEnabledFromWear(context: Context, payload: ByteArray) {
        val appContext = context.applicationContext
        val enabled = payloadJson(payload).optBoolean("enabled", true)
        val data = loadData(appContext)
        val store = ShiftAlarmStore(appContext)
        val updated = data.alarmSettings.copy(enabled = enabled)
        store.save(updated)
        rescheduleShiftAlarms(
            platform = DefaultAlarmPlatformPort(appContext),
            settings = updated,
            savedDays = data.savedDays,
            templateMap = data.templateMap
        )
        publishSnapshot(appContext)
    }

    suspend fun setTemplateAlarmEnabledFromWear(context: Context, payload: ByteArray) {
        val appContext = context.applicationContext
        val json = payloadJson(payload)
        val shiftCode = json.optString("shiftCode").trim()
        val enabled = json.optBoolean("enabled", true)
        if (shiftCode.isBlank()) {
            publishSnapshot(appContext)
            return
        }

        val data = loadData(appContext)
        val template = data.templateMap[shiftCode]
        if (template == null) {
            publishSnapshot(appContext)
            return
        }

        val existing = data.alarmSettings.templateConfigs.associateBy { it.shiftCode }
        val config = (existing[shiftCode] ?: defaultShiftTemplateAlarmConfig(template)).copy(enabled = enabled)
        val updatedConfigs = (data.alarmSettings.templateConfigs.filterNot { it.shiftCode == shiftCode } + config)
            .sortedBy { it.shiftCode }
        val updated = data.alarmSettings.copy(templateConfigs = updatedConfigs)
        ShiftAlarmStore(appContext).save(updated)
        rescheduleShiftAlarms(
            platform = DefaultAlarmPlatformPort(appContext),
            settings = updated,
            savedDays = data.savedDays,
            templateMap = data.templateMap
        )
        publishSnapshot(appContext)
    }

    suspend fun dismissAlarmFromWear(context: Context, payload: ByteArray) {
        val appContext = context.applicationContext
        val alarmKey = payloadJson(payload).optString("alarmKey").ifBlank { "shift_alarm" }
        ShiftAlarmPlaybackService.stop(appContext, alarmKey)
    }

    suspend fun snoozeAlarmFromWear(context: Context, payload: ByteArray) {
        val appContext = context.applicationContext
        val json = payloadJson(payload)
        val alarmKey = json.optString("alarmKey").ifBlank { "shift_alarm" }
        val vibrationType = runCatching {
            ShiftAlarmVibrationType.valueOf(json.optString("vibrationType", ShiftAlarmVibrationType.SYSTEM.name))
        }.getOrElse { ShiftAlarmVibrationType.SYSTEM }
        ShiftAlarmPlaybackService.snooze(
            context = appContext,
            alarmKey = alarmKey,
            title = json.optString("title").ifBlank { "Скоро смена" },
            text = json.optString("text").ifBlank { "Проверь календарь смен" },
            volumePercent = json.optInt("volumePercent", 100).coerceIn(0, 100),
            soundUri = json.optString("soundUri").ifBlank { null },
            soundLabel = json.optString("soundLabel"),
            snoozeIntervalMinutes = json.optInt("snoozeIntervalMinutes", 10).coerceIn(1, 120),
            snoozeCountLimit = json.optInt("snoozeCountLimit", 3).coerceIn(0, 10),
            snoozeCurrentCount = json.optInt("snoozeCurrentCount", 0).coerceAtLeast(0),
            ringDurationSeconds = json.optInt("ringDurationSeconds", 180).coerceIn(10, 3_600),
            rampUpDurationSeconds = json.optInt("rampUpDurationSeconds", 0).coerceIn(0, 180),
            vibrationEnabled = json.optBoolean("vibrationEnabled", true),
            vibrationType = vibrationType,
            vibrationDurationSeconds = json.optInt("vibrationDurationSeconds", 25).coerceIn(0, 300),
            customVibrationPattern = json.optString("customVibrationPattern").trim()
        )
    }

    private suspend fun loadData(context: Context): WearSyncData {
        val db = AppDatabase.getDatabase(context)
        val savedDays = db.shiftDayDao().observeAll().first()
        val templates = db.shiftTemplateDao().observeAll().first()
        val holidays = db.holidayDao().observeByScope("RU-FED").first()
        val workAssignments = WorkAssignmentsStore(context).stateFlow.first()
        val alarmSettings = ShiftAlarmStore(context).settingsFlow.first()
        val payrollSettings = PayrollSettingsStore(context).settingsFlow.first()
        val notes = AppNotesStore(context).notesFlow.first()
        val additionalPayments = AdditionalPaymentsStore(context).paymentsFlow.first()
        val deductions = DeductionsStore(context).deductionsFlow.first()

        val workplaces = workAssignments.workplaces.ifEmpty { defaultWorkplaces() }
        val templateMap = templates.associateBy { it.code }
        val holidayMap = holidays.mapNotNull { holiday ->
            runCatching { LocalDate.parse(holiday.date) to holiday }.getOrNull()
        }.toMap()
        val assignmentsByDate = buildAssignmentsByDate(
            savedDays = savedDays,
            extraAssignmentsByDate = workAssignments.extraAssignmentsByDate,
            workplaces = workplaces
        )

        val payrollInfo = calculateWearPayroll(
            assignmentsByDate = assignmentsByDate,
            templateMap = templateMap,
            holidayMap = holidayMap,
            alarmSettings = alarmSettings,
            payrollSettings = payrollSettings,
            additionalPayments = additionalPayments,
            deductions = deductions
        )

        return WearSyncData(
            savedDays = savedDays,
            templates = templates,
            templateMap = templateMap,
            holidays = holidays,
            holidayMap = holidayMap,
            workAssignments = workAssignments,
            workplaces = workplaces,
            assignmentsByDate = assignmentsByDate,
            alarmSettings = alarmSettings,
            payrollSettings = payrollSettings,
            payrollInfo = payrollInfo,
            notes = notes
        )
    }

    private suspend fun sendMessageToWear(
        context: Context,
        path: String,
        payload: JSONObject
    ) {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) return
        val bytes = payload.toString().toByteArray(Charsets.UTF_8)
        val messageClient = Wearable.getMessageClient(context)
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, path, bytes).await()
        }
    }

    private fun buildSnapshotJson(data: WearSyncData): JSONObject {
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val alarmConfigsByCode = data.alarmSettings.templateConfigs.associateBy { it.shiftCode }
        val upcomingAlarms = ShiftAlarmSchedulerCompat.previewUpcomingAlarms(data)
        val notesByDate = data.notes.groupBy { it.date }

        return JSONObject()
            .put("schemaVersion", 1)
            .put("generatedAt", System.currentTimeMillis())
            .put("today", today.toString())
            .put("month", month.toString())
            .put(
                "todaySummary",
                buildDaySummary(today, data.assignmentsByDate, data.templateMap, data.workplaces, alarmConfigsByCode)
            )
            .put(
                "tomorrowSummary",
                buildDaySummary(today.plusDays(1), data.assignmentsByDate, data.templateMap, data.workplaces, alarmConfigsByCode)
            )
            .put("workplaces", data.workplaces.toJsonArray { workplace ->
                JSONObject()
                    .put("id", workplace.id)
                    .put("name", workplace.name)
            })
            .put("calendar", buildCalendarJson(month, data, alarmConfigsByCode, notesByDate))
            .put("payroll", buildPayrollJson(data.payrollInfo))
            .put("alarms", buildAlarmsJson(data, upcomingAlarms, alarmConfigsByCode))
            .put("notes", buildNotesJson(data.notes))
    }

    private fun buildCalendarJson(
        month: YearMonth,
        data: WearSyncData,
        alarmConfigsByCode: Map<String, ShiftTemplateAlarmConfig>,
        notesByDate: Map<String, List<AppNote>>
    ): JSONArray {
        val today = LocalDate.now()
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            JSONObject()
                .put("date", date.toString())
                .put("day", date.dayOfMonth)
                .put("dayOfWeek", date.dayOfWeek.value)
                .put("isToday", date == today)
                .put("notesCount", notesByDate[date.toString()].orEmpty().size)
                .put("assignments", data.assignmentsByDate[date].orEmpty().toJsonArray { assignment ->
                    buildAssignmentJson(
                        assignment = assignment,
                        template = data.templateMap[assignment.shiftCode],
                        workplaces = data.workplaces,
                        alarmConfig = alarmConfigsByCode[assignment.shiftCode]
                    )
                })
        }.toJsonArray()
    }

    private fun buildAssignmentJson(
        assignment: WearAssignment,
        template: ShiftTemplateEntity?,
        workplaces: List<Workplace>,
        alarmConfig: ShiftTemplateAlarmConfig?
    ): JSONObject {
        val displayCode = stripWorkplaceScopeFromShiftCode(assignment.shiftCode)
        val title = template?.title?.takeIf { it.isNotBlank() } ?: displayCode
        val workplaceName = workplaces.firstOrNull { it.id == assignment.workplaceId }?.name.orEmpty()
        return JSONObject()
            .put("workplaceId", assignment.workplaceId)
            .put("workplaceName", workplaceName)
            .put("shiftCode", assignment.shiftCode)
            .put("displayCode", displayCode)
            .put("title", title)
            .put("iconKey", template?.iconKey?.ifBlank { "TEXT" } ?: "TEXT")
            .put("colorHex", template?.colorHex ?: "#7AE582")
            .put("paidHours", template?.paidHours() ?: 0.0)
            .put("start", alarmConfig?.let { formatClockHm(it.startHour, it.startMinute) }.orEmpty())
            .put("end", alarmConfig?.let { formatClockHm(it.endHour, it.endMinute) }.orEmpty())
    }

    private fun buildPayrollJson(info: WearPayrollInfo): JSONObject {
        return JSONObject()
            .put("periodLabel", info.periodLabel)
            .put("workedHours", info.result.workedHours)
            .put("nightHours", info.result.nightHours)
            .put("grossTotal", info.result.grossTotal)
            .put("netTotal", info.result.netAfterDeductions)
            .put("advance", info.result.netAdvanceAfterDeductions)
            .put("salary", info.result.netSalaryAfterDeductions)
            .put("ndfl", info.result.ndfl)
            .put("advanceDate", info.advanceDate.toString())
            .put("salaryDate", info.salaryDate.toString())
    }

    private fun buildAlarmsJson(
        data: WearSyncData,
        upcomingAlarms: List<ShiftAlarmUpcomingInfo>,
        alarmConfigsByCode: Map<String, ShiftTemplateAlarmConfig>
    ): JSONObject {
        return JSONObject()
            .put("enabled", data.alarmSettings.enabled)
            .put("autoReschedule", data.alarmSettings.autoReschedule)
            .put("wearMirrorEnabled", data.alarmSettings.wearMirrorEnabled)
            .put("wearSoundMode", data.alarmSettings.wearSoundMode.name)
            .put("upcoming", upcomingAlarms.toJsonArray { alarm ->
                JSONObject()
                    .put("triggerAtMillis", alarm.triggerAtMillis)
                    .put("title", alarm.title)
                    .put("text", alarm.text)
                    .put("shiftCode", alarm.shiftCode)
                    .put("alarmKey", alarm.alarmKey)
            })
            .put("templates", data.templates.filter { it.active }.sortedBy { it.sortOrder }.toJsonArray { template ->
                val config = alarmConfigsByCode[template.code]
                JSONObject()
                    .put("shiftCode", template.code)
                    .put("displayCode", stripWorkplaceScopeFromShiftCode(template.code))
                    .put("title", template.title.ifBlank { stripWorkplaceScopeFromShiftCode(template.code) })
                    .put("colorHex", template.colorHex)
                    .put("enabled", config?.enabled ?: false)
                    .put("start", config?.let { formatClockHm(it.startHour, it.startMinute) }.orEmpty())
                    .put("alarmsEnabledCount", config?.alarms.orEmpty().count(ShiftAlarmConfig::enabled))
            })
    }

    private fun buildNotesJson(notes: List<AppNote>): JSONArray {
        return notes
            .sortedWith(compareByDescending<AppNote> { it.date }.thenByDescending { it.updatedAtMillis })
            .take(16)
            .toJsonArray { note ->
                JSONObject()
                    .put("id", note.id)
                    .put("date", note.date)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("updatedAtMillis", note.updatedAtMillis)
            }
    }

    private fun calculateWearPayroll(
        assignmentsByDate: Map<LocalDate, List<WearAssignment>>,
        templateMap: Map<String, ShiftTemplateEntity>,
        holidayMap: Map<LocalDate, HolidayEntity>,
        alarmSettings: ShiftAlarmSettings,
        payrollSettings: PayrollSettings,
        additionalPayments: List<com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment>,
        deductions: List<com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction>
    ): WearPayrollInfo {
        val month = YearMonth.now()
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val timingByCode = alarmSettings.templateConfigs.associateBy { it.shiftCode }
        val periodEntries = assignmentsByDate.entries
            .asSequence()
            .filter { (date, _) -> !date.isBefore(start) && !date.isAfter(end) }
            .flatMap { (date, assignments) -> assignments.asSequence().map { date to it.shiftCode } }
            .toList()
        val shifts = periodEntries.toWorkShiftItems(
            templateMap = templateMap,
            holidayMap = holidayMap,
            settings = payrollSettings,
            timingByCode = timingByCode
        )
        val firstHalfShifts = periodEntries
            .filter { (date, _) -> date.dayOfMonth <= 15 }
            .toWorkShiftItems(
                templateMap = templateMap,
                holidayMap = holidayMap,
                settings = payrollSettings,
                timingByCode = timingByCode
            )
        val resolvedPayments = resolveAdditionalPaymentsForPeriod(
            configuredPayments = additionalPayments,
            startDate = start,
            endDate = end,
            shifts = shifts,
            baseSalary = payrollSettings.baseSalary
        )
        val paymentMultiplier = calculateMonthlyPaymentMultiplierForDateRange(start, end)
        val calculationSettings = payrollSettings.copy(
            housingPayment = payrollSettings.housingPayment * paymentMultiplier
        )
        val payroll = PayrollCalculator.calculate(
            shifts = shifts,
            firstHalfShifts = firstHalfShifts,
            settings = calculationSettings,
            additionalPayments = resolvedPayments.asPayrollPayments(),
            deductions = deductions
        )
        val paymentDates = calculatePaymentDates(
            month = month,
            settings = payrollSettings,
            extraDayOffDates = holidayMap.filterValues { it.isNonWorking }.keys
        )
        return WearPayrollInfo(
            periodLabel = month.format(DateTimeFormatter.ofPattern("MM.yyyy")),
            result = payroll,
            advanceDate = paymentDates.advanceDate,
            salaryDate = paymentDates.salaryDate
        )
    }

    private fun List<Pair<LocalDate, String>>.toWorkShiftItems(
        templateMap: Map<String, ShiftTemplateEntity>,
        holidayMap: Map<LocalDate, HolidayEntity>,
        settings: PayrollSettings,
        timingByCode: Map<String, ShiftTemplateAlarmConfig>
    ): List<WorkShiftItem> {
        return mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = holidayMap,
                applyShortDayReduction = settings.applyShortDayReduction,
                shiftTiming = timingByCode[code]
            )
        }
    }

    private fun buildAssignmentsByDate(
        savedDays: List<ShiftDayEntity>,
        extraAssignmentsByDate: Map<LocalDate, Map<String, String>>,
        workplaces: List<Workplace>
    ): Map<LocalDate, List<WearAssignment>> {
        val grouped = mutableMapOf<LocalDate, MutableMap<String, String>>()
        savedDays.forEach { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@forEach
            grouped.getOrPut(date) { mutableMapOf() }[WORKPLACE_MAIN_ID] = day.shiftCode
        }
        extraAssignmentsByDate.forEach { (date, perWorkplace) ->
            val bucket = grouped.getOrPut(date) { mutableMapOf() }
            perWorkplace.forEach { (workplaceId, shiftCode) ->
                if (workplaceId != WORKPLACE_MAIN_ID && shiftCode.isNotBlank()) {
                    bucket[workplaceId] = shiftCode
                }
            }
        }
        val order = workplaces.map { it.id }
        return grouped.mapValues { (_, perWorkplace) ->
            order.mapNotNull { workplaceId ->
                perWorkplace[workplaceId]?.takeIf { it.isNotBlank() }?.let { code ->
                    WearAssignment(workplaceId = workplaceId, shiftCode = code)
                }
            }
        }
    }

    private fun buildDaySummary(
        date: LocalDate,
        assignmentsByDate: Map<LocalDate, List<WearAssignment>>,
        templateMap: Map<String, ShiftTemplateEntity>,
        workplaces: List<Workplace>,
        alarmConfigsByCode: Map<String, ShiftTemplateAlarmConfig>
    ): String {
        val workplaceNames = workplaces.associate { it.id to it.name }
        val assignments = assignmentsByDate[date].orEmpty()
        if (assignments.isEmpty()) return "смен нет"
        return assignments.joinToString(" • ") { assignment ->
            val template = templateMap[assignment.shiftCode]
            val config = alarmConfigsByCode[assignment.shiftCode]
            val title = template?.title?.takeIf { it.isNotBlank() } ?: stripWorkplaceScopeFromShiftCode(assignment.shiftCode)
            val hours = template?.paidHours()?.takeIf { it > 0.0 }?.let { "${roundOne(it)} ч" }.orEmpty()
            listOf(
                workplaceNames[assignment.workplaceId].orEmpty(),
                title,
                config?.let { formatClockHm(it.startHour, it.startMinute) }.orEmpty(),
                hours
            ).filter { it.isNotBlank() }.joinToString(" ")
        }
    }

    private fun payloadJson(payload: ByteArray): JSONObject {
        if (payload.isEmpty()) return JSONObject()
        return runCatching { JSONObject(String(payload, Charsets.UTF_8)) }.getOrDefault(JSONObject())
    }

    private fun formatTimestamp(timestampMillis: Long): String {
        val dateTime = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return "${dateTime.dayOfMonth.toString().padStart(2, '0')}.${dateTime.monthValue.toString().padStart(2, '0')} ${formatClockHm(dateTime.hour, dateTime.minute)}"
    }

    private fun roundOne(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    private fun <T> Iterable<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        forEach { item -> array.put(transform(item)) }
        return array
    }

    private fun Iterable<JSONObject>.toJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { item -> array.put(item) }
        return array
    }
}

private data class WearSyncData(
    val savedDays: List<ShiftDayEntity>,
    val templates: List<ShiftTemplateEntity>,
    val templateMap: Map<String, ShiftTemplateEntity>,
    val holidays: List<HolidayEntity>,
    val holidayMap: Map<LocalDate, HolidayEntity>,
    val workAssignments: WorkAssignmentsState,
    val workplaces: List<Workplace>,
    val assignmentsByDate: Map<LocalDate, List<WearAssignment>>,
    val alarmSettings: ShiftAlarmSettings,
    val payrollSettings: PayrollSettings,
    val payrollInfo: WearPayrollInfo,
    val notes: List<AppNote>
)

private data class WearAssignment(
    val workplaceId: String,
    val shiftCode: String
)

private data class WearPayrollInfo(
    val periodLabel: String,
    val result: PayrollResult,
    val advanceDate: LocalDate,
    val salaryDate: LocalDate
)

private object ShiftAlarmSchedulerCompat {
    fun previewUpcomingAlarms(data: WearSyncData): List<ShiftAlarmUpcomingInfo> {
        return com.vigilante.shiftsalaryplanner.ShiftAlarmScheduler.previewUpcomingAlarms(
            settings = data.alarmSettings,
            savedDays = data.savedDays,
            templateMap = data.templateMap,
            limit = 5
        )
    }
}
