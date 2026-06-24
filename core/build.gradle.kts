// Pure-Kotlin (JVM) module: the testable business logic — domain model + rules — with no
// Android dependencies. This is what CI compiles and tests, so the unit suite runs WITHOUT the
// Android SDK. (The app modules use compileSdk 37, an Android 17 preview platform that isn't
// published to the public SDK repo CI runners use, so they can't be built on CI yet.)
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
