plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.kairo.plugin.oneui"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.kairo.plugin.oneui"
    minSdk = 26
    targetSdk = 36
    versionCode = 4
    versionName = "1.3.0"
  }
  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}
