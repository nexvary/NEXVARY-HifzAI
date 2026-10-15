plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.nexvary.hifzai"
    compileSdk = 35
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.nexvary.hifzai"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0-dev"
        ndk {
            val proofAbi = providers.gradleProperty("hifzTestAbi").orNull
            if (proofAbi != null) {
                require(proofAbi == "x86_64") { "Only x86_64 is allowed for emulator-only proofs" }
                abiFilters += proofAbi
            } else abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        externalNativeBuild { cmake { arguments += listOf("-DANDROID_STL=c++_static") } }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/asr-assets"))
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

/**
 * Fail closed: no Release APK/AAB until scholarly, Quran source, rights,
 * security, legal and device-level sign-off is independently verified.
 * Commit with a reviewed release policy change only AFTER approvals.
 */
val blockUnapprovedPublicRelease = tasks.register("blockUnapprovedPublicRelease") {
    group = "verification"
    doLast {
        throw org.gradle.api.GradleException(
            "RELEASE BLOCKED: Quran/content and legal review not approved. Use debug only for expert review."
        )
    }
}
tasks.configureEach {
    if (name in setOf("assembleRelease", "bundleRelease", "packageRelease")) {
        dependsOn(blockUnapprovedPublicRelease)
    }
}

// Test tooling only; never shipped in the application.
dependencies { androidTestImplementation("androidx.test:rules:1.6.1") }

// Host build step only. Runtime ASR reads the bundled, digest-checked local model.
val prepareLocalWhisperModel = tasks.register<Exec>("prepareLocalWhisperModel") {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/prepare_whisper_model.py")
    inputs.file(rootProject.file("tools/prepare_whisper_model.py"))
    outputs.file(layout.buildDirectory.file("generated/asr-assets/asr/ggml-tiny.bin"))
    outputs.file(layout.buildDirectory.file("generated/asr-assets/licenses/Android-NDK-NOTICE.txt"))
}
tasks.named("preBuild") { dependsOn(prepareLocalWhisperModel) }
