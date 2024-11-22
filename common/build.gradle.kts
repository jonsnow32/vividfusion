plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  kotlin("plugin.serialization")
  id("maven-publish")
}

android {
  namespace = "cloud.app.vvf.common"
  compileSdk = 35

  defaultConfig {
    minSdk = 21
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  android {
    publishing {
      singleVariant("release") {
        withSourcesJar()
        withJavadocJar()
      }
    }
  }
}

dependencies {
  api(libs.androidx.core.ktx)
  api(libs.squareup.okhttp)
  // parse html
  api(libs.jsoup);
  api(libs.timber)
  api(libs.kotlinx.serialization.json.v171)
}

// run command in terminal to publish to maven local "./gradlew publishToMavenLocal"
afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("release") {
        from(components["release"])
        groupId = "cloud.app"
        artifactId = "common"
        version = "1.0"
      }
    }
  }
}

