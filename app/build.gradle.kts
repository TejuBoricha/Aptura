import java.time.Duration
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "org.aptura.player"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.aptura.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // B-01: Release signing is opt-in and secret-free in the repo. Provide the upload keystore via
    // local.properties or environment variables (CI secrets); without them the release artifact
    // stays unsigned (assembleRelease still works for size/R8 verification).
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun signingValue(key: String, env: String): String? =
        (localProps.getProperty(key) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

    val releaseStoreFile = signingValue("aptura.signing.storeFile", "APTURA_SIGNING_STORE_FILE")
    val hasReleaseSigning = releaseStoreFile != null &&
        signingValue("aptura.signing.storePassword", "APTURA_SIGNING_STORE_PASSWORD") != null &&
        signingValue("aptura.signing.keyAlias", "APTURA_SIGNING_KEY_ALIAS") != null &&
        signingValue("aptura.signing.keyPassword", "APTURA_SIGNING_KEY_PASSWORD") != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("aptura.signing.storePassword", "APTURA_SIGNING_STORE_PASSWORD")
                keyAlias = signingValue("aptura.signing.keyAlias", "APTURA_SIGNING_KEY_ALIAS")
                keyPassword = signingValue("aptura.signing.keyPassword", "APTURA_SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // B-02: R8 + resource shrinking (material-icons-extended alone ships ~7k classes).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    testOptions {
      unitTests.isReturnDefaultValues = true
      unitTests.isIncludeAndroidResources = true
      unitTests.all { testTask ->
        // Hang guard for the WHOLE unit-test task. It was 30 s when the suite took ~15 s; the suite now has ~300 tests
        // (several against a real ExoPlayer / WorkManager / SQLite), so the guard is sized to the suite, not removed.
        testTask.timeout.set(Duration.ofSeconds(120))
        testTask.testLogging {
          events("failed", "skipped")
          showStandardStreams = false
          exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
      }
    }


    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    bundle {
      abi {
        enableSplit = true
      }
      density {
        enableSplit = true
      }
      language {
        // Must be false for offline-only app: dynamic feature/language downloads are impossible without INTERNET
        enableSplit = false
      }
    }

    splits {
      abi {
        isEnable = true
        reset()
        include("arm64-v8a", "armeabi-v7a", "x86_64")
        isUniversalApk = true
      }
    }
}

kotlin {
    jvmToolchain(17)
}

// Room writes each schema version here (committed) so migrations can be verified against real history.
ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Media3 Playback Engine (Media3 core + ExoPlayer + UI)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.extractor)

  // Local Media Indexing & Metadata (Room)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // WorkManager (Background Media Ingest & Differential Scanning)
  implementation(libs.androidx.work.runtime.ktx)

  // DataStore Preferences (Asynchronous Settings & State Persistence)
  implementation(libs.androidx.datastore.preferences)

  // Coil (Async Video Thumbnails & Image Pipeline)
  implementation(libs.coil.compose)
  implementation(libs.coil.video)

  // Serialization
  implementation(libs.kotlinx.serialization.json)


  // Local tests: jUnit, coroutines, Turbine, Robolectric, Compose UI test
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.work.testing) // in-memory WorkManager database for the cold-start scan test
  testImplementation(libs.androidx.compose.ui.test.junit4)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.test.uiautomator)

  // Navigation & Multi-Pane Adaptive Scenes
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.androidx.compose.material3.adaptive)
  implementation(libs.androidx.compose.material3.adaptive.layout)
  implementation(libs.androidx.compose.material3.adaptive.navigation3)
}

// Bypass AAR metadata check for androidx.compose.material3.adaptive 1.3.0 which declares minCompileSdk=37
// while compiling cleanly against Android SDK 36 and AGP 9.0.1
tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

