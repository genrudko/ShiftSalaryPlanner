package com.vigilante.shiftsalaryplanner.wearsync

object WearSyncContract {
    const val SNAPSHOT_PATH = "/shift_salary_planner/snapshot"
    const val PATH_REQUEST_SNAPSHOT = "/shift_salary_planner/request_snapshot"
    const val PATH_ADD_NOTE = "/shift_salary_planner/add_note"
    const val PATH_TOGGLE_ALL_ALARMS = "/shift_salary_planner/toggle_all_alarms"
    const val PATH_TOGGLE_TEMPLATE_ALARM = "/shift_salary_planner/toggle_template_alarm"
    const val PATH_ASSISTANT_PROMPT = "/shift_salary_planner/assistant_prompt"
    const val PATH_ALARM_RING = "/shift_salary_planner/alarm_ring"
    const val PATH_ALARM_STOP = "/shift_salary_planner/alarm_stop"
    const val PATH_ALARM_SNOOZE = "/shift_salary_planner/alarm_snooze"
    const val PATH_ALARM_DISMISS_FROM_WEAR = "/shift_salary_planner/alarm_dismiss_from_wear"
    const val PATH_ALARM_SNOOZE_FROM_WEAR = "/shift_salary_planner/alarm_snooze_from_wear"

    const val KEY_SNAPSHOT_JSON = "snapshot_json"
    const val KEY_GENERATED_AT = "generated_at"
}
