package com.example.invoicekeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.invoicekeeper.ui.InvoiceKeeperApp
import com.example.invoicekeeper.ui.theme.InvoiceKeeperTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            InvoiceKeeperTheme {
                InvoiceKeeperApp()
            }
        }
    }
}
