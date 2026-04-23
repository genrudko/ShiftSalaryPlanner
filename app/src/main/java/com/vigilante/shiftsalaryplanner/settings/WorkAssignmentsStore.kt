package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

const val WORKPLACE_MAIN_ID = "work_main"
const val WORKPLACE_SECOND_ID = "work_second"
const val WORKPLACE_THIRD_ID = "work_third"

data class Workplace(
    val id: String,
    val name: String
)

data class WorkAssignmentsState(
    val workplaces: List<Workplace>,
    val extraAssignmentsByDate: Map<LocalDate, Map<String, String>>
)

data class WorkplaceDateShiftAssignment(
    val date: LocalDate,
    val workplaceId: String,
    val shiftCode: String
)

class WorkAssignmentsStore(context: Context) {

    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _stateFlow = MutableStateFlow(load())
    val stateFlow: Flow<WorkAssignmentsState> = _stateFlow.asStateFlow()

    fun setShiftForDate(
        workplaceId: String,
        date: LocalDate,
        shiftCode: String?
    ) {
        if (workplaceId == WORKPLACE_MAIN_ID) return
        val current = _stateFlow.value
        val mutable = current.extraAssignmentsByDate
            .mapValues { (_, value) -> value.toMutableMap() }
            .toMutableMap()
        val dateMap = mutable[date]?.toMutableMap() ?: mutableMapOf()
        val normalizedCode = shiftCode?.trim().orEmpty()

        if (normalizedCode.isBlank()) {
            dateMap.remove(workplaceId)
        } else {
            dateMap[workplaceId] = normalizedCode
        }

        if (dateMap.isEmpty()) {
            mutable.remove(date)
        } else {
            mutable[date] = dateMap
        }

        save(
            current.copy(
                extraAssignmentsByDate = mutable.mapValues { it.value.toMap() }
            )
        )
    }

    fun clearDateRange(startDate: LocalDate, endDate: LocalDate) {
        val from = minOf(startDate, endDate)
        val to = maxOf(startDate, endDate)
        val current = _stateFlow.value
        val filtered = current.extraAssignmentsByDate.filterKeys { date ->
            date.isBefore(from) || date.isAfter(to)
        }
        save(current.copy(extraAssignmentsByDate = filtered))
    }

    fun clearAll() {
        val current = _stateFlow.value
        save(current.copy(extraAssignmentsByDate = emptyMap()))
    }

    fun renameWorkplace(
        workplaceId: String,
        newName: String
    ): Boolean {
        val normalizedName = newName.trim()
        if (normalizedName.isBlank()) return false

        val current = _stateFlow.value
        var changed = false
        val updatedWorkplaces = current.workplaces.map { workplace ->
            if (workplace.id == workplaceId) {
                changed = true
                workplace.copy(name = normalizedName)
            } else {
                workplace
            }
        }

        if (!changed) return false
        save(current.copy(workplaces = updatedWorkplaces))
        return true
    }

    fun findAssignmentsByShiftCode(shiftCode: String): List<WorkplaceDateShiftAssignment> {
        if (shiftCode.isBlank()) return emptyList()
        val current = _stateFlow.value
        return buildList {
            current.extraAssignmentsByDate.forEach { (date, assignments) ->
                assignments.forEach { (workplaceId, code) ->
                    if (code == shiftCode) {
                        add(
                            WorkplaceDateShiftAssignment(
                                date = date,
                                workplaceId = workplaceId,
                                shiftCode = code
                            )
                        )
                    }
                }
            }
        }
    }

    fun replaceShiftCode(
        oldShiftCode: String,
        newShiftCode: String
    ) {
        val oldCode = oldShiftCode.trim()
        val newCode = newShiftCode.trim()
        if (oldCode.isBlank() || newCode.isBlank() || oldCode == newCode) return

        val current = _stateFlow.value
        var changed = false
        val updated = current.extraAssignmentsByDate.mapValues { (_, assignments) ->
            assignments.mapValues { (_, code) ->
                if (code == oldCode) {
                    changed = true
                    newCode
                } else {
                    code
                }
            }
        }

        if (changed) {
            save(current.copy(extraAssignmentsByDate = updated))
        }
    }

    fun removeShiftCode(shiftCode: String): List<WorkplaceDateShiftAssignment> {
        val targetCode = shiftCode.trim()
        if (targetCode.isBlank()) return emptyList()

        val removed = findAssignmentsByShiftCode(targetCode)
        if (removed.isEmpty()) return emptyList()

        val current = _stateFlow.value
        val updated = current.extraAssignmentsByDate.mapValues { (_, assignments) ->
            assignments.filterValues { code -> code != targetCode }
        }.filterValues { assignments -> assignments.isNotEmpty() }

        save(current.copy(extraAssignmentsByDate = updated))
        return removed
    }

    fun restoreAssignments(assignments: List<WorkplaceDateShiftAssignment>) {
        if (assignments.isEmpty()) return
        assignments.forEach { item ->
            setShiftForDate(
                workplaceId = item.workplaceId,
                date = item.date,
                shiftCode = item.shiftCode
            )
        }
    }

    private fun save(state: WorkAssignmentsState) {
        prefs.edit {
            putString(KEY_WORKPLACES_JSON, serializeWorkplaces(state.workplaces))
            putString(KEY_EXTRA_ASSIGNMENTS_JSON, serializeExtraAssignments(state.extraAssignmentsByDate))
        }
        _stateFlow.value = state
    }

    private fun load(): WorkAssignmentsState {
        val storedWorkplaces = parseWorkplaces(prefs.getString(KEY_WORKPLACES_JSON, null))
        val normalizedWorkplaces = normalizeWorkplaces(storedWorkplaces)
        val assignments = parseExtraAssignments(
            raw = prefs.getString(KEY_EXTRA_ASSIGNMENTS_JSON, null),
            allowedWorkplaceIds = normalizedWorkplaces.map { it.id }.toSet()
        )
        return WorkAssignmentsState(
            workplaces = normalizedWorkplaces,
            extraAssignmentsByDate = assignments
        )
    }

    private fun normalizeWorkplaces(raw: List<Workplace>): List<Workplace> {
        val defaults = defaultWorkplaces()
        if (raw.isEmpty()) return defaults
        val byId = raw.associateBy { it.id }.toMutableMap()
        defaults.forEach { fallback ->
            if (byId[fallback.id] == null) {
                byId[fallback.id] = fallback
            }
        }
        return defaults.map { fallback ->
            val saved = byId[fallback.id]
            if (saved == null) {
                fallback
            } else {
                saved.copy(name = saved.name.trim().ifBlank { fallback.name })
            }
        }
    }

    private fun parseWorkplaces(raw: String?): List<Workplace> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val name = item.optString("name").trim()
                    if (id.isBlank() || name.isBlank()) continue
                    add(Workplace(id = id, name = name))
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun parseExtraAssignments(
        raw: String?,
        allowedWorkplaceIds: Set<String>
    ): Map<LocalDate, Map<String, String>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                val dateKeys = root.keys()
                while (dateKeys.hasNext()) {
                    val dateKey = dateKeys.next()
                    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: continue
                    val perDate = root.optJSONObject(dateKey) ?: continue
                    val parsedDateAssignments = buildMap {
                        val workplaceKeys = perDate.keys()
                        while (workplaceKeys.hasNext()) {
                            val workplaceId = workplaceKeys.next()
                            if (workplaceId !in allowedWorkplaceIds || workplaceId == WORKPLACE_MAIN_ID) continue
                            val code = perDate.optString(workplaceId).trim()
                            if (code.isNotBlank()) {
                                put(workplaceId, code)
                            }
                        }
                    }
                    if (parsedDateAssignments.isNotEmpty()) {
                        put(date, parsedDateAssignments)
                    }
                }
            }
        }.getOrElse { emptyMap() }
    }

    private fun serializeWorkplaces(workplaces: List<Workplace>): String {
        val array = JSONArray()
        workplaces.forEach { workplace ->
            array.put(
                JSONObject().apply {
                    put("id", workplace.id)
                    put("name", workplace.name)
                }
            )
        }
        return array.toString()
    }

    private fun serializeExtraAssignments(assignments: Map<LocalDate, Map<String, String>>): String {
        val root = JSONObject()
        assignments
            .toSortedMap()
            .forEach { (date, perWork) ->
                val item = JSONObject()
                perWork
                    .toSortedMap()
                    .forEach { (workplaceId, code) ->
                        if (workplaceId != WORKPLACE_MAIN_ID && code.isNotBlank()) {
                            item.put(workplaceId, code)
                        }
                    }
                if (item.length() > 0) {
                    root.put(date.toString(), item)
                }
            }
        return root.toString()
    }

    companion object {
        private const val PREFS_NAME = "work_assignments"
        private const val KEY_WORKPLACES_JSON = "workplaces_json"
        private const val KEY_EXTRA_ASSIGNMENTS_JSON = "extra_assignments_json"
    }
}

fun defaultWorkplaces(): List<Workplace> = listOf(
    Workplace(id = WORKPLACE_MAIN_ID, name = "Работа 1"),
    Workplace(id = WORKPLACE_SECOND_ID, name = "Работа 2"),
    Workplace(id = WORKPLACE_THIRD_ID, name = "Работа 3")
)
