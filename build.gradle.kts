// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.hilt) apply false
    id("com.github.ben-manes.versions") version "0.54.0"
    id("nl.littlerobots.version-catalog-update") version "0.8.4"
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        val stableKeywords = listOf("RELEASE", "FINAL", "GA")
        val isStable = stableKeywords.any { candidate.version.uppercase().contains(it) }
            || Regex("^[0-9,.v-]+(-r)?$").matches(candidate.version)
        !isStable
    }
}