plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.kairo.plugin.zh"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.kairo.plugin.zh"
    minSdk = 26
    targetSdk = 36
    versionCode = 2
    versionName = "1.1.0"
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
