package com.example.invoicekeeper.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.invoicekeeper.data.remote.ApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val webhookUrl: String,
    val baseUrl: String,
) {
    val hasWebhook: Boolean get() = webhookUrl.isNotBlank()
}

/**
 * Two short strings, read on every screen and written from one. DataStore Preferences is the right
 * size of tool for this; the invoices themselves live in Room.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val BASE_URL = stringPreferencesKey("base_url")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            webhookUrl = prefs[Keys.WEBHOOK_URL].orEmpty(),
            baseUrl = prefs[Keys.BASE_URL]?.takeIf { it.isNotBlank() } ?: ApiClient.DEFAULT_BASE_URL,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { it[Keys.WEBHOOK_URL] = url.trim() }
    }

    suspend fun setBaseUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        context.dataStore.edit {
            it[Keys.BASE_URL] = cleaned.ifBlank { ApiClient.DEFAULT_BASE_URL }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
