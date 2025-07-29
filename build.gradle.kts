plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false // This is good, Hilt plugin declared
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.secrets) apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.5" apply false
    jacoco
}

    jacoco {
        toolVersion = "0.8.12" // Versión actual y compatible con Gradle 8.4
    }


