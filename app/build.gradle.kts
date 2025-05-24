plugins {
    // Plugins de Android y Kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Plugins para Hilt y Google Services
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.secrets)
    kotlin("kapt")
}

android {
    namespace = "com.cursoandroid.queermap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cursoandroid.queermap"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Configuración para pruebas
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        // Habilitar View Binding
        viewBinding = true
    }
}

dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.db)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.coroutines)

    // Autenticación
    implementation(libs.auth)
    implementation(libs.google.auth)
    implementation(libs.facebook.login)

    // Google Maps y Places
    implementation(libs.maps)
    implementation(libs.places)

    // Retrofit y GSON
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Otros
    implementation(libs.picasso)
    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.cardview)
    implementation(libs.core.splashscreen)
    implementation(libs.hilt.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    kapt(libs.hilt.compiler)


    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.kotest.assertions)

    // Pruebas UI
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso)
}
