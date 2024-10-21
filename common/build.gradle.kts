plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.kotlin.parcelize)
  id("maven-publish")
}

android {
  namespace = "cloud.app.common"
  compileSdk = 34

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
  implementation(libs.androidx.core.ktx)
  implementation(libs.squareup.okhttp)
  // parse html
  implementation(libs.jsoup);
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

