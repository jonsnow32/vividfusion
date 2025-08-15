pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")

    // IronSource repository
    maven("https://android-sdk.is.com/")

    // Vungle repository
    maven("https://s3.amazonaws.com/moat-sdk-builds")
  }
}

rootProject.name = "ViVidFusion"
include(":app")
include(":common")
