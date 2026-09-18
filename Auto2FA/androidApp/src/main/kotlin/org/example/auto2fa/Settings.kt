package org.example.auto2fa

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val serverIp: String = "",
    val serverPort: Int = 8080,
    val publicKeyBase64: String = "",
)

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            serverIp = prefs[Keys.SERVER_IP] ?: "",
            serverPort = prefs[Keys.SERVER_PORT] ?: 8080,
            publicKeyBase64 = prefs[Keys.PUBLIC_KEY] ?: "",
        )
    }

    suspend fun save(settings: Settings) {
        dataStore.edit { prefs ->
            prefs[Keys.SERVER_IP] = settings.serverIp
            prefs[Keys.SERVER_PORT] = settings.serverPort
            prefs[Keys.PUBLIC_KEY] = settings.publicKeyBase64
        }
    }

    private object Keys {
        val SERVER_IP = stringPreferencesKey("server_ip")
        val SERVER_PORT = intPreferencesKey("server_port")
        val PUBLIC_KEY = stringPreferencesKey("public_key")
    }
}
