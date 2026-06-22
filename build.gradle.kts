// Top-level build file. Plugins are declared here with `apply false` so each
// module can opt in via the version catalog (gradle/libs.versions.toml).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    // AGP 9 has built-in Kotlin, but the Compose compiler plugin must be declared
    // with a version (at least here or in the catalog) to be resolved from repositories.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}
