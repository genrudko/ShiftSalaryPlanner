package com.vigilante.shiftsalaryplanner.wear.sync

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

data class WearSnapshotUiState(
    val snapshot: WearSnapshot = WearSnapshot(),
    val commandStatus: String = "Подключаюсь к телефону",
    val connectedNodes: Int = 0,
    val loading: Boolean = true
)

class WearSnapshotRepository(context: Context) : DataClient.OnDataChangedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val _state = MutableStateFlow(WearSnapshotUiState())

    val state: StateFlow<WearSnapshotUiState> = _state

    fun start() {
        dataClient.addListener(this)
        loadCachedSnapshot()
        refreshFromDataLayer()
        requestSnapshot()
    }

    fun close() {
        dataClient.removeListener(this)
        scope.cancel()
    }

    fun requestSnapshot() {
        sendMessage(
            path = WearSyncContract.PATH_REQUEST_SNAPSHOT,
            payload = JSONObject(),
            pending = "Обновляю данные"
        )
    }

    fun addNote(body: String) {
        sendMessage(
            path = WearSyncContract.PATH_ADD_NOTE,
            payload = JSONObject().put("body", body),
            pending = "Сохраняю заметку"
        )
    }

    fun setAllAlarmsEnabled(enabled: Boolean) {
        sendMessage(
            path = WearSyncContract.PATH_TOGGLE_ALL_ALARMS,
            payload = JSONObject().put("enabled", enabled),
            pending = if (enabled) "Включаю будильники" else "Выключаю будильники"
        )
    }

    fun setTemplateAlarmEnabled(shiftCode: String, enabled: Boolean) {
        sendMessage(
            path = WearSyncContract.PATH_TOGGLE_TEMPLATE_ALARM,
            payload = JSONObject()
                .put("shiftCode", shiftCode)
                .put("enabled", enabled),
            pending = "Меняю будильник"
        )
    }

    fun askAssistant(prompt: String) {
        sendMessage(
            path = WearSyncContract.PATH_ASSISTANT_PROMPT,
            payload = JSONObject().put("prompt", prompt),
            pending = "Спрашиваю ассистента"
        )
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            if (item.uri.path != WearSyncContract.SNAPSHOT_PATH) return@forEach
            val raw = DataMapItem.fromDataItem(item)
                .dataMap
                .getString(WearSyncContract.KEY_SNAPSHOT_JSON)
                .orEmpty()
            if (raw.isBlank()) return@forEach
            applySnapshot(raw, "Обновлено")
        }
    }

    private fun loadCachedSnapshot(): Boolean {
        val raw = WearSnapshotCache.loadRaw(appContext)
        if (raw.isBlank()) return false
        return applySnapshot(raw, "Сохранённые данные")
    }

    private fun refreshFromDataLayer() {
        scope.launch {
            runCatching {
                val dataItems = dataClient.dataItems.await()
                try {
                    var rawSnapshot = ""
                    dataItems.forEach { item ->
                        if (item.uri.path == WearSyncContract.SNAPSHOT_PATH) {
                            rawSnapshot = DataMapItem.fromDataItem(item)
                                .dataMap
                                .getString(WearSyncContract.KEY_SNAPSHOT_JSON)
                                .orEmpty()
                        }
                    }
                    rawSnapshot
                } finally {
                    dataItems.release()
                }
            }.onSuccess { raw ->
                if (raw.isNotBlank()) {
                    applySnapshot(raw, "Сохранённые данные")
                } else if (!_state.value.snapshot.hasSyncedData()) {
                    _state.value = _state.value.copy(loading = false)
                }
            }.onFailure {
                if (!_state.value.snapshot.hasSyncedData()) {
                    _state.value = _state.value.copy(loading = false)
                }
            }
        }
    }

    private fun applySnapshot(raw: String, status: String): Boolean {
        return runCatching { WearSnapshot.fromJson(raw) }
            .onSuccess { snapshot ->
                WearSnapshotCache.save(appContext, raw)
                _state.value = _state.value.copy(
                    snapshot = snapshot,
                    commandStatus = status,
                    loading = false
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    commandStatus = error.message ?: "Не удалось прочитать данные",
                    loading = false
                )
            }
            .isSuccess
    }

    private fun sendMessage(path: String, payload: JSONObject, pending: String) {
        _state.value = _state.value.copy(commandStatus = pending)
        scope.launch {
            runCatching {
                val nodes = nodeClient.connectedNodes.await()
                _state.value = _state.value.copy(connectedNodes = nodes.size)
                if (nodes.isEmpty()) error("Телефон не подключен")
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, bytes).await()
                }
            }.onSuccess {
                _state.value = _state.value.copy(commandStatus = "Команда отправлена")
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    commandStatus = offlineStatus(error),
                    loading = false
                )
            }
        }
    }

    private fun offlineStatus(error: Throwable): String {
        val message = error.message ?: "Телефон не ответил"
        return if (_state.value.snapshot.hasSyncedData()) {
            "$message · сохранённые данные"
        } else {
            message
        }
    }

    private fun WearSnapshot.hasSyncedData(): Boolean {
        return generatedAt > 0L ||
            calendar.isNotEmpty() ||
            notes.isNotEmpty() ||
            payroll.periodLabel.isNotBlank() ||
            alarms.upcoming.isNotEmpty() ||
            alarms.templates.isNotEmpty()
    }
}
