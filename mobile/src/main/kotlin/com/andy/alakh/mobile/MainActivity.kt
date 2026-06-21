package com.andy.alakh.mobile

import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Intentional stub. Its only job for now is to establish the phone-side package
 * (applicationId com.andy.alakh) so the watch and phone can pair over the Wear OS
 * Data Layer later. Build out the real companion UI here when you add phone sync.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this).apply {
            text = "Alakh companion\n(coming soon)"
            textSize = 20f
            gravity = Gravity.CENTER
        }
        setContentView(view)
    }
}
