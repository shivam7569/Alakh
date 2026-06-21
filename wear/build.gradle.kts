plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.andy.alakh"

    // compileSdk 36 (Android 16) is chosen as the most likely-installed stable platform.
    // If Gradle sync says a dependency "requires compileSdk 37 or later", bump this to 37
    // and install that platform via Tools > SDK Manager. See README "Step 2".
    compileSdk = 36

    defaultConfig {
        // SAME applicationId as the :mobile module. Together with the shared debug keystore
        // (~/.android/debug.keystore, used by default on this machine for both modules) this
        // lets the watch and phone apps recognize each other over the Wear OS Data Layer later.
        applicationId = "com.andy.alakh"
        minSdk = 34          // Wear OS 5.0 floor; Pixel Watch 3 runs 5.1 (API 35)
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose (BOM aligns the androidx.compose.* artifacts)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Wear Compose (Material3)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.wear.ongoing)

    // Health Services (workout sensors) + Data Layer (future phone pairing)
    implementation(libs.androidx.health.services)
    implementation(libs.play.services.wearable)

    // Core / lifecycle / activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
}
