package com.vigilante.shiftsalaryplanner.payroll.datastore

import androidx.datastore.core.Serializer
import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Сериализатор для сохранения настроек начислений в DataStore
 */
object AccrualSettingsSerializer : Serializer<List<AccrualSettings>> {

    override val defaultValue: List<AccrualSettings>
        get() = DefaultAccrualConfig.getDefaultSettings()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    override suspend fun readFrom(input: InputStream): List<AccrualSettings> {
        return try {
            val jsonString = input.readBytes().decodeToString()
            json.decodeFromString(ListSerializer(AccrualSettings.serializer()), jsonString)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: List<AccrualSettings>, output: OutputStream) {
        val jsonString = json.encodeToString(ListSerializer(AccrualSettings.serializer()), t)
        output.write(jsonString.encodeToByteArray())
    }
}