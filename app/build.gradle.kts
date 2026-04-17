// ─────────────────────────────────────────────────────────────────────────────
// :app — build.gradle.kts
// The application module. Entry point. Wires together all feature modules.
// ─────────────────────────────────────────────────────────────────────────────
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// ── Read signing credentials from local.properties (never hardcode) ───────────
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

// Prefixed with "ks" to avoid name collisions inside signingConfigs block
val ksPath     = localProperties.getProperty("KEYSTORE_PATH")
val ksPassword = localProperties.getProperty("KEYSTORE_PASSWORD")
val ksAlias    = localProperties.getProperty("KEY_ALIAS")
val ksKeyPass  = localProperties.getProperty("KEY_PASSWORD")

val hasSigningConfig = ksPath != null &&
                       ksPassword != null &&
                       ksAlias != null &&
                       ksKeyPass != null

android {
    namespace = "com.lorevyn.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lorevyn.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile     = file(ksPath!!)
                storePassword = ksPassword
                keyAlias      = ksAlias
                keyPassword   = ksKeyPass
            }
        }
    }

    buildTypes {
        release {
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.activity.compose)
    implementation(libs.startup)
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.navigation)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    implementation(libs.work.runtime.ktx)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Coil 3 — needed here because LorevynApplication implements
    // SingletonImageLoader.Factory to configure app-wide MemoryCache/DiskCache
    // bounds (security audit, April 2026). Feature modules also depend on Coil
    // for AsyncImage usage; this declaration is specifically for the
    // ImageLoader builder + MemoryCache/DiskCache classes used in
    // LorevynApplication.newImageLoader().
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:reading"))
    implementation(project(":feature:book-detail"))
    implementation(project(":feature:add-book"))
    implementation(project(":feature:library"))
    implementation(project(":feature:discovery"))
    implementation(project(":feature:migration"))
    implementation(project(":feature:journey"))
    implementation(project(":feature:share"))
    implementation(project(":feature:billing"))

    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:security"))

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
