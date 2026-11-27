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
    versionCode = 1
    versionName = "1.0.0"
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}
