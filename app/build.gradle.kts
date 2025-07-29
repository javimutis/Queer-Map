// build.gradle.kts (app module)

// --- INICIO DE IMPORTS REQUERIDOS POR GRADLE Y JACOCO ---
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.api.tasks.testing.Test // Importar Test task

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
            // Habilitar la cobertura para tests unitarios y de instrumentación en la variante debug
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // La configuración Jacoco específica para los Test tasks se hará en tasks.withType<Test>().configureEach
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
    testImplementation(libs.robolectric)


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
    testImplementation(libs.play.services.tasks)
}

// Configura Jacoco para todas las tareas de Test (incluyendo testDebugUnitTest)
tasks.withType<Test>().configureEach {
    // Accede a la extensión Jacoco de la tarea de test de forma explícita
    // Esto asegura que el contexto sea JacocoTaskExtension
    extensions.configure(JacocoTaskExtension::class.java) {
        // ¡Usar los métodos setters explícitos!
        setIncludeNoLocationClasses(true)
        setExcludes(listOf("jdk.internal.*")) // setExcludes toma una lista
    }
}

// Task para generar el reporte de cobertura de tests unitarios
tasks.register<JacocoReport>("jacocoTestReport") {
    // Asegura que los tests unitarios se ejecuten antes de generar el reporte
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        // Define la ubicación de salida para el reporte HTML
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport"))
        csv.required.set(false)
    }

    // Define las clases que se incluirán en el reporte (código fuente compilado)
    // Excluye archivos generados y de boilerplate
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug") {
            // Todas las cadenas de exclusión deben usar COMILLAS DOBLES
            exclude(
                // Exclusiones de Hilt/Dagger, R, BuildConfig, Manifest
                "**/*_Hilt*",
                "****/Dagger*",
                "**/*Module*",
                "**/*_Factory*",
                "**/*_MembersInjector*",
                "**/*Bindings*",
                "**/*EntryPoint*",
                "**/*_Provide*",
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                // Excluye las propias clases de test (para que no aparezcan en la cobertura del código de la app)
                "**/*Test*.*",
                // Excluye clases del framework Android (no son parte de tu código de app)
                "android/**/*.*",
                // Excluye clases de respuesta/entidades de datos (generalmente no tienen lógica ejecutable)
                "**/data/remote/responses/**",
                "**/data/local/entities/**",
                "**/model/User*.*", // Asumiendo que User es una data class sin lógica compleja
                // Exclusiones de Data Binding y View Binding
                "**/*Binding.class",
                "**/*ViewBinding.class",
                "**/*DataBindingInfo",
                "**/*databinding*",
                // Exclusiones de componentes UI (Actividades, Fragmentos, Adaptadores, Diálogos, etc.)
                // Esto es para enfocar los unit tests en la lógica de negocio y dejar la UI para instrumentados.
                "**/*Activity*",
                "**/*Fragment*",
                "**/*Adapter*",
                "**/*Dialog*",
                "**/*Application*",
                "**/*Navigator*",
                // Exclusiones de código generado por Kotlin/JVM (lambdas, inlined functions, etc.)
                "**/*Kt\$WhenMappings*",
                "**/*\$inlined*",
                "**/*lambda\$*",
                "**/*\$\$ExternalSyntheticAPI*",
                "**/*\$jacocoInit",
                "**/*\$default",
                // Exclusión de la clase Application principal
                "**/QueerMapApp.class"
            )
        }.plus(
            fileTree("${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
                // Mismas exclusiones para las clases Kotlin compiladas
                exclude(
                    "**/*_Hilt*",
                    "****/Dagger*",
                    "**/*Module*",
                    "**/*_Factory*",
                    "**/*_MembersInjector*",
                    "**/*Bindings*",
                    "**/*EntryPoint*",
                    "**/*_Provide*",
                    "**/R.class",
                    "**/R\$*.class",
                    "**/BuildConfig.*",
                    "**/Manifest*.*",
                    "**/*Test*.*",
                    "android/**/*.*",
                    "**/data/remote/responses/**",
                    "**/data/local/entities/**",
                    "**/model/User*.*",
                    "**/*Binding.class",
                    "**/*ViewBinding.class",
                    "**/*DataBindingInfo",
                    "**/*databinding*",
                    "**/*Activity*",
                    "**/*Fragment*",
                    "**/*Adapter*",
                    "**/*Dialog*",
                    "**/*Application*",
                    "****/*Navigator*",
                    "**/*Kt\$WhenMappings*",
                    "**/*\$inlined*",
                    "**/*lambda\$*",
                    "**/*\$\$ExternalSyntheticAPI*",
                    "**/*\$jacocoInit",
                    "**/*\$default",
                    "**/QueerMapApp.class"
                )
            }
        )
    )

    // Define los directorios que contienen el código fuente (para el reporte HTML, muestra el código)
    sourceDirectories.setFrom(
        files(
            "$projectDir/src/main/java",
            "$projectDir/src/main/kotlin"
        )
    )

    // Define los archivos .exec que contienen los datos de ejecución de las pruebas
    // ¡Esta es la parte más crítica y donde suele estar el error!
    // Se buscan los archivos .exec generados por testDebugUnitTest.
    executionData.setFrom(
        fileTree(layout.buildDirectory.get().asFile) {
            // Todas las cadenas de inclusión deben usar COMILLAS DOBLES
            include(
                "jacoco/testDebugUnitTest.exec", // Ruta común en algunas configuraciones JaCoCo
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec" // Ruta MUY común para AGP
            )
        }
    )
}

// Task para verificar la cobertura (opcional, pero útil para CI/CD)
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport") // Depende del reporte para asegurar que los datos estén actualizados

    violationRules {
        rule {
            element = "BUNDLE" // Aplica la regla a todo el módulo (bundle)

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
