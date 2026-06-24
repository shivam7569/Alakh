plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.andy.alakh.shared"
    compileSdk = 37

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
    // Pure-Kotlin domain model + rules live in :core. `api` re-exports them so the
    // Room/data code here and consumers like :wear keep seeing the same package unchanged.
    api(project(":core"))

    implementation(libs.androidx.core.ktx)

    // Room (on-device history). Processor runs via KSP.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Wear Data Layer (routine/session sync between phone and watch — used by both app modules).
    implementation(libs.play.services.wearable)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
