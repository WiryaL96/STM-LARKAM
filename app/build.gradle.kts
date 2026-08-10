import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // Enables the Compose compiler and turns on the compose build feature.
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    // NOTE: no org.jetbrains.kotlin.android — AGP 9 built-in Kotlin handles Kotlin.
}

// Apply the Google Services plugin ONLY when google-services.json is present, so the
// app still builds without a Firebase config. At runtime the app then falls back to a
// local in-memory repository (see ServiceLocator). Drop your google-services.json into
// app/ to switch automatically to real Firebase Realtime Database.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.lifecycle(
        "Larkam: google-services.json not found — building WITHOUT Firebase " +
            "(app will use the local in-memory repository)."
    )
}

android {
    namespace = "com.wiryadinata.stmlarkam"
    // AndroidX libraries (core 1.19.0, lifecycle 2.11.0, ...) require compiling
    // against SDK 37+. targetSdk stays at 36.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.wiryadinata.stmlarkam"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Built-in Kotlin (AGP 9): configure the Kotlin compiler via the kotlin { } extension.
// android { kotlinOptions { } } is not available with built-in Kotlin.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // AndroidX / Material (Views) — kept for the XML app theme used by the window.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.material)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Firebase Realtime Database (BOM-managed versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.compose.bom))
}
