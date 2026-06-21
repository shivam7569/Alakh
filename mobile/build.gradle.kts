plugins {
    alias(libs.plugins.android.application)
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    // Data Layer client — present now so the companion is ready to pair with the watch.
    implementation(libs.play.services.wearable)
}
