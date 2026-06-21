package com.andy.alakh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.andy.alakh.health.PermissionHelper
import com.andy.alakh.presentation.AlakhApp

class MainActivity : ComponentActivity() {

    // Results are not consumed here; each feature re-checks its own permissions before use.
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for the workout-related runtime permissions up front. The breathing
        // exercise needs none of these, so the app is fully usable even if denied.
        val missing = PermissionHelper.missingPermissions(this)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }

        setContent { AlakhApp() }
    }
}
