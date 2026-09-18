package org.example.auto2fa

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val serverIp: String = "",
    val publicKeyBase64: String = "",
    val tlsCertificateBase64: String = "",
)

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            serverIp = prefs[Keys.SERVER_IP] ?: "",
            publicKeyBase64 = prefs[Keys.PUBLIC_KEY] ?: "",
            tlsCertificateBase64 = prefs[Keys.TLS_CERTIFICATE] ?: "",
        )
    }

    suspend fun save(settings: Settings) {
        dataStore.edit { prefs ->
            prefs[Keys.SERVER_IP] = settings.serverIp
            prefs[Keys.PUBLIC_KEY] = settings.publicKeyBase64
            prefs[Keys.TLS_CERTIFICATE] = settings.tlsCertificateBase64
        }
    }

    private object Keys {
        val SERVER_IP = stringPreferencesKey("server_ip")
        val PUBLIC_KEY = stringPreferencesKey("public_key")
        val TLS_CERTIFICATE = stringPreferencesKey("tls_certificate")
    }
}
