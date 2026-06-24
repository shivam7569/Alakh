package com.andy.alakh.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.andy.alakh.mobile.ui.AlakhPhoneTheme
import com.andy.alakh.mobile.ui.PhoneApp

/** The phone companion: plan routines, review history, and sync to Google Health. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlakhPhoneTheme { PhoneApp() }
        }
    }
}
