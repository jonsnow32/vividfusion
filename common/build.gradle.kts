plugins {
  id("java-library")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  id("maven-publish")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  api(libs.squareup.okhttp)
  api(libs.jsoup)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialization.json.v171)
}

afterEvaluate {
  publishing {
    publications {
      create<MavenPublication>("mavenJava") {
        groupId = "cloud.app.vvf"
        artifactId = "common"
        version = "1.0"
        from(components["java"])
      }
    }
  }
}

