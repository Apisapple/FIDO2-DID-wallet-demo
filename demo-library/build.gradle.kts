import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs

val isSnapshot = (project.findProperty("snapshotBuild") as? String)?.toBoolean() ?: false
val versionNameStr =
    if (isSnapshot) {
        "${project.findProperty("versionMajor")}.${project.findProperty("versionMinor")}.${
            project.findProperty(
                "versionPatch",
            )
        }-SNAPSHOT"
    } else {
        "${project.findProperty("versionMajor")}.${project.findProperty("versionMinor")}.${
            project.findProperty(
                "versionPatch",
            )
        }"
    }

plugins {
    id("com.android.library") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    id("de.mannodermaus.android-junit5") version "1.11.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("maven-publish")
    id("signing")
}

group = "com.raonsecure"
version = versionNameStr

val coreVersion = (findProperty("coreVersion") as? String) ?: "1.13.1"
val appcompatVersion = (findProperty("appcompatVersion") as? String) ?: "1.7.0"
val kotlinxCoroutinesVersion = (findProperty("kotlinxCoroutinesVersion") as? String) ?: "1.8.1"
val kotlinTestJunitVersion = (findProperty("kotlinTestJunitVersion") as? String) ?: "2.0.21"
val espressoVersion = (findProperty("espressoVersion") as? String) ?: "3.6.1"
val mockkVersion = (findProperty("mockkVersion") as? String) ?: "1.13.12"
val fragmentTestingVersion = (findProperty("fragmentTestingVersion") as? String) ?: "1.8.5"
val junit5Version = (findProperty("junit5Version") as? String) ?: "5.11.4"
val truthVersion = (findProperty("truthVersion") as? String) ?: "1.4.4"
val biometricVersion = (findProperty("biometricVersion") as? String) ?: "1.1.0"
val roomVersion = (findProperty("roomVersion") as? String) ?: "2.6.1"
val kotlinxSerializationVersion =
    (findProperty("kotlinxSerializationVersion") as? String) ?: "1.7.3"

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.raonsecure"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    publishing {
        singleVariant("release") {}
    }
}

dependencies {
    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    // library for test
    androidTestImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinTestJunitVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
    androidTestImplementation("io.mockk:mockk-android:$mockkVersion")
    androidTestImplementation("androidx.fragment:fragment-testing:$fragmentTestingVersion")
    testImplementation("io.mockk:mockk-android:$mockkVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinTestJunitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")

    // junit5
    testImplementation(platform("org.junit:junit-bom:$junit5Version"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")

    androidTestImplementation(platform("org.junit:junit-bom:$junit5Version"))
    androidTestImplementation("org.junit.jupiter:junit-jupiter-api")
    androidTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    androidTestImplementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")

    // truth
    androidTestImplementation("com.google.truth:truth:$truthVersion")
    testImplementation("com.google.truth:truth:$truthVersion")

    // cbor
    implementation("co.nstant.in:cbor:0.9")

    // coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")

    // androidx biometric
    implementation("androidx.biometric:biometric:$biometricVersion")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

tasks.withType<KaptGenerateStubs>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.withType<Zip>().configureEach {
    if (name.startsWith("bundle") && name.endsWith("Aar")) {
        val variantName =
            name.removePrefix("bundle").removeSuffix("Aar").replaceFirstChar { it.lowercase() }
        archiveFileName.set("webauthn-kotlin-$variantName.aar")
    }
}

// Generate sources and javadoc JARs
val androidSourcesJar =
    tasks.register<Jar>("androidSourcesJar") {
        archiveClassifier.set("sources")
        from(android.sourceSets["main"].java.srcDirs)
    }

val androidJavadocJar =
    tasks.register<Jar>("androidJavadocJar") {
        archiveClassifier.set("javadoc")
        from("docs/javadoc")
    }

afterEvaluate {
    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(androidSourcesJar)
                artifact(androidJavadocJar)

                groupId = "com.linecorp"
                artifactId = "webauthn-kotlin"
                version = versionNameStr

                pom {
                    name.set(artifactId)
                    description.set("A Kotlin library for implementing WebAuthn authentication in Android applications")
                    url.set("https://github.com/line/webauthn-kotlin")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            name.set("LY Corporation")
                            email.set("dl_oss_dev@linecorp.com")
                            url.set("https://engineering.linecorp.com/en/")
                        }
                    }

                    scm {
                        connection.set("scm:git@github.com:line/webauthn-kotlin.git")
                        developerConnection.set("scm:git:ssh://github.com/line/webauthn-kotlin.git")
                        url.set("https://github.com/line/webauthn-kotlin")
                    }
                }
            }
        }
    }

    extensions.configure<SigningExtension> {
        val signingKeyId = findProperty("signingKeyId") as String?
        val signingKey = findProperty("signingKey") as String?
        val signingPassword = findProperty("signingPassword") as String?

        if (!signingKeyId.isNullOrBlank() && !signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            val publishing = extensions.getByType(PublishingExtension::class.java)
            sign(publishing.publications["release"])
        }
    }
}
