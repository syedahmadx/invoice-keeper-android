package com.example.invoicekeeper

import android.app.Application
import com.example.invoicekeeper.data.local.AppDatabase
import com.example.invoicekeeper.data.local.SettingsStore
import com.example.invoicekeeper.data.remote.ApiClient
import com.example.invoicekeeper.data.repository.InvoiceRepository

/**
 * Manual dependency wiring. The graph is three objects deep, so a DI framework would cost more
 * build time than it saves.
 */
class InvoiceKeeperApplication : Application() {

    val repository: InvoiceRepository by lazy {
        InvoiceRepository(
            api = ApiClient.apiService,
            dao = AppDatabase.get(this).invoiceDao(),
            settingsStore = SettingsStore(this),
        )
    }
}
