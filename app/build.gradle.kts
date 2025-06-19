// build.gradle.kts (app module)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt) // Asegúrate de que este plugin esté aplicado
    alias(libs.plugins.google.services)
    alias(libs.plugins.secrets)
    kotlin("kapt") // Este también es crucial para Hilt
    id("androidx.navigation.safeargs.kotlin")
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

        testInstrumentationRunner = "com.cursoandroid.queermap.CustomTestRunner"
        multiDexEnabled = true

        testInstrumentationRunnerArguments.putAll(
            mapOf(
                // La siguiente línea es crucial para Hilt en las pruebas.
                // Asegúrate de que el paquete sea correcto si HiltTestApplication no está en el raíz.
                "dagger.hilt.android.testing.HiltTestApplication_Application" to "com.cursoandroid.queermap.HiltTestApplication"
            )
        )
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
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
        viewBinding = true
    }

    packaging {
        resources {
            pickFirsts += "META-INF/LICENSE.md"
            pickFirsts += "META-INF/LICENSE.txt"
            pickFirsts += "META-INF/NOTICE.md"
            pickFirsts += "META-INF/NOTICE.txt"
            pickFirsts += "META-INF/*.kotlin_module"
            pickFirsts += "META-INF/AL2.0"
            pickFirsts += "META-INF/LGPL2.1"
            pickFirsts += "META-INF/licenses/ASM"
            pickFirsts += "META-INF/LICENSE-notice.md"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Core Android y Kotlin
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.cardview)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.multidex)

    // Firebase (usando la BoM para gestionar versiones)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.db)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.coroutines)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Autenticación
    implementation(libs.google.auth)
    implementation(libs.facebook.login)

    // Mapas y Lugares
    implementation(libs.maps)
    implementation(libs.places)

    // Red y JSON
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // Navegación
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Carga de imágenes
    implementation(libs.picasso)

    // Hilt **¡Asegúrate de que estas dos líneas estén presentes y correctas!**
    implementation(libs.hilt.core) // Esta es la dependencia que busca el plugin.
    kapt(libs.hilt.compiler)       // El procesador de anotaciones.

    // --- DEPENDENCIAS DE PRUEBAS ---

    // Pruebas Unitarias
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.kotest.assertions)

    // Pruebas de Instrumentación (Android Tests)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.arch.core.testing)

    // Espresso
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.espresso.idling.resource)

    // MockK para Android Tests
    androidTestImplementation(libs.mockk.android)

    // Hilt para Pruebas de Instrumentación
    androidTestImplementation(libs.hilt.android.testing)
    // El procesador de anotaciones para Hilt en las pruebas de instrumentación
    kaptAndroidTest(libs.hilt.compiler)

    // Fragment Testing
    androidTestImplementation(libs.androidx.fragment.testing)

// Navigation Testing (for TestNavHostController)
    androidTestImplementation(libs.androidx.navigation.testing)
    // Google Truth for assertions
    androidTestImplementation(libs.google.truth)
}