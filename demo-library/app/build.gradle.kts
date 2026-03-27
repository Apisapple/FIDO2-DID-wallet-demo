plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    // JUnit 5를 Android 환경에서 실행하기 위한 필수 플러그인
    id("de.mannodermaus.android-junit5") version "2.0.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

android {
    namespace = "com.example.demo.library"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // JUnit 5를 위한 커스텀 Runner 설정
        testInstrumentationRunnerArguments["runnerBuilder"] = "de.mannodermaus.junit5.AndroidJUnit5Builder"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    // Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.biometric)
    implementation(libs.cbor)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Local Unit Tests (JUnit 5)
    testImplementation(libs.mockk.android)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(platform(libs.jupiter.bom))
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)

    // Android Instrumented Tests
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.jupiter.bom))
    androidTestImplementation(libs.jupiter.api)
}
