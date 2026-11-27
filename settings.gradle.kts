pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("androidx.*")
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google {
      content {
        includeGroupByRegex("androidx.*")
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
      }
    }
    mavenCentral()
    maven("https://maven.pkg.github.com/tribalfs/*") {
      content { includeGroupByRegex("sesl\\..*") }
      // Reuse the workstation's package registry access without copying secrets into Kairo.
      val packageCredentials = java.util.Properties().apply {
        val file = listOf(".config/kairo/github.properties", ".config/twidget/github.properties")
          .map { File(System.getProperty("user.home"), it) }.firstOrNull { it.isFile }
        file?.inputStream()?.use { load(it) }
      }
      credentials {
        username = System.getenv("GH_PACKAGES_USER") ?: packageCredentials.getProperty("ghUsername", "")
        password = System.getenv("GH_PACKAGES_TOKEN") ?: packageCredentials.getProperty("ghAccessToken", "")
      }
    }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "GemmA"

include(":app", ":oneui-theme-plugin", ":chinese-lang-plugin")
