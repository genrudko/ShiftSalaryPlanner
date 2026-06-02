package com.vigilante.shiftsalaryplanner.wear.alarm

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

object WearAlarmCommandSender {
    suspend fun send(context: Context, path: String, payload: JSONObject) {
        val nodes = Wearable.getNodeClient(context.applicationContext).connectedNodes.await()
        val bytes = payload.toString().toByteArray(Charsets.UTF_8)
        nodes.forEach { node ->
            Wearable.getMessageClient(context.applicationContext)
                .sendMessage(node.id, path, bytes)
                .await()
        }
    }
}
