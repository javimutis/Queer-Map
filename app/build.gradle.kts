// build.gradle.kts (app module)

// --- INICIO DE IMPORTS REQUERIDOS POR GRADLE Y JACOCO ---
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
// --- FIN DE IMPORTS ---

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.secrets)
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("jacoco") // <--- Asegúrate de que el plugin de JaCoCo esté aplicado aquí
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
                "dagger.hilt.android.testing.HiltTestApplication_Application" to "com.cursoandroid.queermap.HiltTestApplication"
            )
        )
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
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
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn"
        )
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

    // --- CONFIGURACIÓN DE TESTS Y JACOCO PARA UNIT TESTS ---
    testOptions {
        unitTests.all {
            jacoco {
                // NOTA: 'includeNoLocationClasses' no es accesible directamente aquí.
                // Es una limitación conocida con algunas versiones de Gradle/JaCoCo y funciones inline de Kotlin.
                // Tus tests para onSuccess/onFailure son correctos lógicamente,
                // pero JaCoCo puede no reportar el 100% debido a cómo se instrumenta el bytecode de las funciones inline.
            }
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
    implementation(libs.androidx.fragment.ktx)

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

    // Hilt
    implementation(libs.hilt.core)
    kapt(libs.hilt.compiler)

    // --- DEPENDENCIAS DE PRUEBAS ---

    // Pruebas Unitarias
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.kotest.assertions)

    // ¡¡¡NUEVA LÍNEA CRÍTICA!!!
    // Esto es crucial para que MockK funcione correctamente con KAPT en unit tests,
    // especialmente para mockkObject y mocking de clases finales.
    kaptTest(libs.mockk.agent.jvm) // <--- ¡AÑADE ESTO!

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
    kaptAndroidTest(libs.hilt.compiler)

    // Fragment Testing
    androidTestImplementation(libs.androidx.fragment.testing)
    debugImplementation(libs.androidx.fragment.testing)

    // Navigation Testing (for TestNavHostController)
    androidTestImplementation(libs.androidx.navigation.testing)
    // Google Truth for assertions
    androidTestImplementation(libs.google.truth)
    testImplementation(kotlin("test"))
}


// --- INICIO DE LA CONFIGURACIÓN DE JACOCO MEJORADA Y CORREGIDA ---

// Tarea para generar el reporte de cobertura de JaCoCo
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport"))
        csv.required.set(false)
    }

    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug") {
            exclude(
                // --- EXCLUSIONES COMUNES PARA ANDROID Y KOTLIN ---
                "**/*_Hilt*", "****/Dagger*", "**/*Module*", "**/*_Factory*",
                "**/*_MembersInjector*", "**/*Bindings*", "**/*EntryPoint*", "**/*_Provide*",
                "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
                "**/*Test*.*", "android/**/*.*",
                "**/data/remote/responses/**", "**/data/local/entities/**",
                "**/*Binding.class", "**/*ViewBinding.class", "**/*DataBindingInfo", "**/*databinding*",
                "**/*Activity*", "**/*Fragment*", "**/*Adapter*", "**/*Dialog*",
                "**/*Application*", "**/*Navigator*", "**/*Event*", "**/*State*",
                // Excluir clases generadas por el compilador Kotlin para inline functions que JaCoCo a veces malinterpreta.
                // Estas pueden aparecer con nombres como 'YourClass$lambda$1', 'YourClass$WhenMappings', 'YourClass$$inlined'
                "**/*Kt\$WhenMappings*", "**/*\$inlined*", "**/*lambda\$*", "**/*\$\$ExternalSyntheticAPI*",
                "**/*\$jacocoInit", "**/*\$default",
                "**/*ViewModel\$*get*$", "**/*ViewModel\$*set*$", // getters/setters auto-generados de ViewModels
                // Tus exclusiones específicas del dominio
                "**/model/User*.*", "**/QueerMapApp.class"
            )
        }.plus( // Incluir también las clases Kotlin desde su propio directorio intermedio
            fileTree("${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
                exclude(
                    // Repite las mismas exclusiones para las clases Kotlin
                    "**/*_Hilt*", "****/Dagger*", "**/*Module*", "**/*_Factory*",
                    "**/*_MembersInjector*", "**/*Bindings*", "**/*EntryPoint*", "**/*_Provide*",
                    "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
                    "**/*Test*.*", "android/**/*.*",
                    "**/data/remote/responses/**", "**/data/local/entities/**",
                    "**/*Binding.class", "**/*ViewBinding.class", "**/*DataBindingInfo", "**/*databinding*",
                    "**/*Activity*", "**/*Fragment*", "**/*Adapter*", "**/*Dialog*",
                    "**/*Application*", "****/*Navigator*", "**/*Event*", "**/*State*",
                    // Excluir clases generadas por el compilador Kotlin para inline functions
                    "**/*Kt\$WhenMappings*", "**/*\$inlined*", "**/*lambda\$*", "**/*\$\$ExternalSyntheticAPI*",
                    "**/*\$jacocoInit", "**/*\$default",
                    "**/*ViewModel\$*get*$", "**/*ViewModel\$*set*$",
                    "**/model/User*.*", "**/QueerMapApp.class"
                )
            }
        )
    )

    sourceDirectories.setFrom(
        files(
            "$projectDir/src/main/java",
            "$projectDir/src/main/kotlin"
        )
    )

    executionData.setFrom(
        fileTree(layout.buildDirectory.get().asFile).include(
            "jacoco/testDebugUnitTest.exec", // Para tests unitarios
            "**/*.exec" // Incluye cualquier otro archivo .exec generado
        )
    )
}

// --- OPCIONAL: Configuración para jacocoTestCoverageVerification ---
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    violationRules {
        rule {
            element = "BUNDLE"

            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = 0.65.toBigDecimal()
            }
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = 0.70.toBigDecimal()
            }
        }
    }
}
