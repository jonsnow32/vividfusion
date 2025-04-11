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

kotlin {
  jvmToolchain(17)
}

dependencies {
  api(libs.squareup.okhttp)
  api(libs.jsoup)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialize.json)
}


// Generate a VVFBuildConfig class with the version
tasks.named("processResources") {
  val version = project.version.toString()
  val targetDir = file("${layout.buildDirectory}/generated/source/kotlin/main/cloud/app/vvf/common")
  doLast {
    targetDir.mkdirs()
    file("$targetDir/VVFBuildConfig.kt").writeText("""
            package cloud.app.vvf.common

            object VVFBuildConfig {
                const val LIB_VERSION = "$version"
            }
        """.trimIndent())
  }
}

// Include generated sources in the source set
sourceSets {
  main {
    kotlin.srcDir("${layout.buildDirectory}/generated/source/kotlin/main")
  }
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

