plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.andy.alakh.mobile"
    compileSdk = 37

    defaultConfig {
        // SAME applicationId as :wear so the phone and watch apps pair over the Data Layer.
        applicationId = "com.andy.alakh"
        minSdk = 34          // a phone could go lower; kept aligned with :wear for simplicity
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose (BOM aligns the androidx.compose.* artifacts) + standard Material3 for the phone.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room (to access AlakhDatabase for routines + synced history)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Core / lifecycle / activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Data Layer client — pair with the watch.
    implementation(libs.play.services.wearable)
}
