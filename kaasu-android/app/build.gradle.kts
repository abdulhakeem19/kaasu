import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.kaasu.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kaasu.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // SHA-256 of the release signing certificate. Public by nature: `apksigner verify
        // --print-certs` prints it from any copy of the APK, so it is a fingerprint to compare
        // against, never a secret. The app shows it so the value on a phone can be read against
        // the one published in the README.
        buildConfigField(
            "String",
            "EXPECTED_SIGNING_SHA256",
            "\"a286824792e66616ed51eaad4a6bba90b619bae770c51b323f7c51b36e5141c9\""
        )
    }

    signingConfigs {
        // Release signing pulled from keystore.properties (kept out of version control).
        // See docs/archive/play-store/RELEASE_CHECKLIST.md for how to create the keystore + properties file.
        val keystorePropsFile = rootProject.file("keystore.properties")
        if (keystorePropsFile.exists()) {
            val props = Properties().apply { load(keystorePropsFile.inputStream()) }
            create("release") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("Boolean", "ENABLE_PARSER_LOGS", "true")
        }
        release {
            // Minify/shrink intentionally off for the first release (avoids reflection keep-rule
            // risk with Room/Hilt/JSON). Revisit once stable.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "ENABLE_PARSER_LOGS", "false")
            // Use the release signing config when keystore.properties is present.
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // MigrationTest reads the exported schemas as its fixtures, so they have to be packaged
    // into the test APK's assets.
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    testOptions {
        unitTests {
            // The capture pipeline logs through android.util.Log, which throws "not mocked" on the
            // JVM. Returning defaults lets the real pipeline be tested without wrapping every log
            // call in an interface that exists only for tests.
            isReturnDefaultValues = true
        }
    }
}

// AGP 9 ships Kotlin support itself, so jvmTarget moves out of the removed android.kotlinOptions
// DSL and onto the Kotlin extension directly.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

dependencies {
    // Room's migration-test helpers read the exported schema JSON through kotlinx.serialization.
    // room-testing brings serialization-json 1.8.1, while serialization-core arrives transitively
    // at 1.7.3 via androidx.lifecycle and is then pinned onto the androidTest classpath by AGP's
    // consistent resolution with the app's runtime. json 1.8.1 calling into core 1.7.3 throws
    // AbstractMethodError before a single line of migration SQL runs, so every migration test
    // failed for a reason that had nothing to do with migrations.
    //
    // A constraint rather than a dependency: it raises the version already on the classpath
    // without adding an edge, and raising core (rather than lowering json) keeps the app and its
    // instrumented tests on one version, which is the point of consistent resolution.
    constraints {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1") {
            because("room-testing's serialization-json 1.8.1 requires core 1.8.x")
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // App lock (biometric + PIN)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager (+ Hilt integration for @HiltWorker/@AssistedInject — SmsBackfillWorker)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Charts
    implementation(libs.vico.compose.m3)

    // PDF text extraction (statement import)
    implementation(libs.pdfbox.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
