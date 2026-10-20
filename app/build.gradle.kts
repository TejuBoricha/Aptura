import java.time.Duration

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.aptura.player"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.aptura.player"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
      unitTests.all { testTask ->
        testTask.timeout.set(Duration.ofSeconds(30))
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

  // WorkManager (Background Media Ingest & Differential Scanning)
  implementation(libs.androidx.work.runtime.ktx)

  // DataStore Preferences (Asynchronous Settings & State Persistence)
  implementation(libs.androidx.datastore.preferences)

  // Coil (Async Video Thumbnails & Image Pipeline)
  implementation(libs.coil.compose)
  implementation(libs.coil.video)

  // Serialization
  implementation(libs.kotlinx.serialization.json)


  // Local tests: jUnit, coroutines, Turbine
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
