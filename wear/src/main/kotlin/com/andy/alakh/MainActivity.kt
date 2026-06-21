package com.andy.alakh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.andy.alakh.health.PermissionHelper
import com.andy.alakh.presentation.AlakhApp
import com.andy.alakh.shared.data.AlakhDatabase
import com.andy.alakh.shared.data.CatalogSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        // One-time: load the bundled exercise catalog into the local database (background thread).
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                CatalogSeeder.seedIfEmpty(
                    applicationContext,
                    AlakhDatabase.get(applicationContext).exerciseDao(),
                )
            }
        }

        setContent { AlakhApp() }
    }
}
