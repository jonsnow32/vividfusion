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

group = "cloud.app.vvf.common"
version = "1.0"

kotlin {
  jvmToolchain(17)
}

dependencies {
  api(libs.squareup.okhttp)
  api(libs.jsoup)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialization.json.v171)
}


publishing {
  publications {
    create<MavenPublication>("maven") { // Explicitly create a publication
      from(components["java"]) // Include the Java/Kotlin library component
      groupId = "cloud.app.vvf.common" // Consistent groupId
      artifactId = "common" // Define the artifactId
      version = project.version.toString()
    }
  }
}

