plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.footprinttracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.footprinttracker"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.03.00"))
    implementation("androidx.activity:activity-compose:1.12.0-alpha08")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0-alpha01")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("com.google.maps.android:maps-compose:8.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
