plugins {
    alias(libs.plugins.android.application)
    // Jetpack Compose compiler. We provide a version in the catalog that
    // aligns with the Kotlin version bundled with AGP 9.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.andy.alakh"

    // compileSdk 36 (Android 16). If Gradle sync says a dependency "requires compileSdk 37
    // or later", bump this and install that platform via Tools > SDK Manager. See README.
    compileSdk = 36

    defaultConfig {
        // SAME applicationId as the :mobile module so the watch & phone apps can pair
        // over the Wear OS Data Layer later (they also share the default debug keystore).
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
        // With AGP 9 built-in Kotlin, the Kotlin jvmTarget defaults to targetCompatibility,
        // so setting Java 17 here covers both Java and Kotlin — no separate kotlin {} block needed.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
