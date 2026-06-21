// Top-level build file. Plugins are declared here with `apply false` so each
// module can opt in via the version catalog (gradle/libs.versions.toml).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    // No kotlin-android plugin: AGP 9 has built-in Kotlin support. The Compose
    // compiler plugin is applied (versionless) directly in :wear, so it isn't
    // declared here.
}
