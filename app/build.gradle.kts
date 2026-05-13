plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.3.21"
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.hilt)
}

fun getVersionNameFromGit(): String {
    return try {
        val process = ProcessBuilder("git", "tag", "--points-at", "HEAD")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(10, TimeUnit.SECONDS)
        process.inputStream.bufferedReader().readText().trim()
    } catch (_: Exception) {
        "1.0.0" // Default fallback if git command fails
    }.trim().let {
        if (it.isEmpty()) {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            process.waitFor(10, TimeUnit.SECONDS)
            process.inputStream.bufferedReader().readText().trim()
        } else {
            it
        }

    }
}

fun getVersionCodeFromGit(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(10, TimeUnit.SECONDS)
        process.inputStream.bufferedReader().readText().trim().toInt()
    } catch (_: Exception) {
        1 // Default fallback if git command fails
    }
}

android {
    namespace = "com.gtw.filamentmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gtw.filamentmanager"
        minSdk = 33
        targetSdk = 36
        versionCode = getVersionCodeFromGit()
        versionName = getVersionNameFromGit()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        val projectName = rootProject.name.replace(" ", "_")
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                output.versionName.map { versionName ->
                    "${projectName}-${variant.name}-${versionName}.apk"
                }
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.icons.extended)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hivemq.mqtt.client)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    testImplementation(platform(libs.junit))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitRunner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
