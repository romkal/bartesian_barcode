plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  kotlin("plugin.serialization")
}

android {
  namespace = "us.romkal.barcode"
  compileSdk = 35

  defaultConfig {
    applicationId = "us.romkal.barcode"
    minSdk = 24
    targetSdk = 35
    versionCode = 12
    versionName = "0.12"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.viewfinder)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.constraintlayout.compose)
  implementation(libs.coil.network.okhttp)
  implementation(libs.coil.compose)
  implementation(libs.okhttp)
  implementation(libs.coil.network.cache.control)
  implementation(libs.androidx.lifecycle.runtime.android)
  implementation(libs.okhttp.coroutines)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}