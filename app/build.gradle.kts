plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.nexvary.hifzai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexvary.hifzai"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0-dev"
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
