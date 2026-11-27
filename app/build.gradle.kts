plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.kairo.agent"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.kairo.agent"
    minSdk = 26
    targetSdk = 36
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    versionCode = 31
    versionName = "1.4.1"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("debug")
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

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

// SESL retains AndroidX class names. Resolve one implementation of each View module.
// Compose Material and the app's window theme remain stock; native controls are plugin-gated.
configurations.configureEach {
  exclude(group = "androidx.core", module = "core")
  exclude(group = "androidx.core", module = "core-ktx")
  exclude(group = "androidx.customview", module = "customview")
  exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
  exclude(group = "androidx.drawerlayout", module = "drawerlayout")
  exclude(group = "androidx.viewpager2", module = "viewpager2")
  exclude(group = "androidx.viewpager", module = "viewpager")
  exclude(group = "androidx.appcompat", module = "appcompat")
  exclude(group = "androidx.fragment", module = "fragment")
  exclude(group = "androidx.fragment", module = "fragment-ktx")
  exclude(group = "androidx.preference", module = "preference")
  exclude(group = "androidx.recyclerview", module = "recyclerview")
  exclude(group = "androidx.swiperefreshlayout", module = "swiperefreshlayout")
  exclude(group = "androidx.slidingpanelayout", module = "slidingpanelayout")
  exclude(group = "sesl.androidx.core", module = "core-ktx")
}

dependencies {
  implementation(libs.oneui.icons)
  implementation(libs.sesl.appcompat)
  implementation(libs.sesl.core)
  constraints {
    implementation(libs.sesl.appcompat)
    implementation(libs.sesl.coordinatorlayout)
    implementation(libs.sesl.core)
    implementation(libs.sesl.customview)
    implementation(libs.sesl.drawerlayout)
    implementation(libs.sesl.fragment)
    implementation(libs.sesl.indexscroll)
    implementation(libs.sesl.material)
    implementation(libs.sesl.picker.app)
    implementation(libs.sesl.picker.basic)
    implementation(libs.sesl.picker.color)
    implementation(libs.sesl.preference)
    implementation(libs.sesl.recyclerview)
    implementation(libs.sesl.slidingpanelayout)
    implementation(libs.sesl.swiperefreshlayout)
    implementation(libs.sesl.viewpager)
    implementation(libs.sesl.viewpager2)
  }

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
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

  // On-Device GenAI (AICore / Gemma 4) & Health Connect
  implementation(libs.google.mlkit.genai.prompt)
  implementation(libs.androidx.health.connect)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

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
