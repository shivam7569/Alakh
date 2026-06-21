plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.andy.alakh.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 34
    }

    compileOptions {
        // AGP 9 built-in Kotlin: jvmTarget follows targetCompatibility, so Java 17 here is enough.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Room (on-device history). Processor runs via KSP.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
