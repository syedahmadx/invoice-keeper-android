package com.example.invoicekeeper.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.invoicekeeper.InvoiceKeeperApplication
import com.example.invoicekeeper.data.repository.InvoiceRepository

/**
 * Gives every ViewModel factory a one-line path to the app graph, without a DI framework.
 */
val CreationExtras.app: InvoiceKeeperApplication
    get() = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
        "Application missing from CreationExtras"
    } as InvoiceKeeperApplication

val CreationExtras.repository: InvoiceRepository get() = app.repository

val CreationExtras.androidApplication: Application get() = app
