import java.io.ByteArrayOutputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
}

dependencies {
  implementation(project(":common"))
  implementation(libs.androidx.core.ktx)
}


val extType: String = "database"
val extId: String = "test-stream-client"
val extClass: String = "cloud.app.avp.extension.SampleClient"

val extIconUrl: String? =
  "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png"
val extName: String = "Extension Sample"
val extDescription: String? = "A sample of stream client"

val extAuthor: String = "Jonsnow32"
val extAuthorUrl: String? =  "https://github.com/jonsnow32/"

val extRepoUrl: String? =  "https://github.com/jonsnow32/StreamExpand-Ext"
val extUpdateUrl: String? =  "https://github.com/jonsnow32/StreamExpand-Ext"

val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()
val verCode = gitCount
val verName = gitHash


tasks.register("uninstall") {
  exec {
    isIgnoreExitValue = true
    executable(android.adbExecutable)
    args("shell", "pm", "uninstall", android.defaultConfig.applicationId!!)
  }
}

android {
  namespace = "cloud.app.sampleextensionclient"
  compileSdk = 35

  defaultConfig {
    applicationId = "cloud.app.sampleextensionclient"
    minSdk = 24
    targetSdk = 35

    manifestPlaceholders.apply {
      put("type", "cloud.app.avp.extension.${extType}")
      put("id", extId)
      put("class_path", "cloud.app.avp.extension.${extClass}")
      put("version", verName)
      put("version_code", verCode.toString())
      extIconUrl?.let { put("icon_url", it) }
      put("app_name", "Echo : $extName Extension")
      put("name", extName)
      extDescription?.let { put("description", it) }
      put("author", extAuthor)
      extAuthorUrl?.let { put("author_url", it) }
      extRepoUrl?.let { put("repo_url", it) }
      extUpdateUrl?.let { put("update_url", it) }
    }
    resValue("string", "id", extId)
    resValue("string", "class_path", "$namespace.${extClass}")

    versionName = verName
    resValue("string", "version", verName)
    versionCode = verCode
    resValue("string", "version_code", verCode.toString())

    extIconUrl?.let { resValue("string", "icon_url", it) }
    resValue("string", "app_name", "Echo : $extName Extension")
    resValue("string", "name", extName)
    description?.let { resValue("string", "description", it) }

    resValue("string", "author", extAuthor)
    extAuthorUrl?.let { resValue("string", "author_url", it) }

    extRepoUrl?.let { resValue("string", "repo_url", it) }
    extUpdateUrl?.let { resValue("string", "update_url", it) }
  }

  buildTypes {
    all {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

fun execute(vararg command: String): String {
  val outputStream = ByteArrayOutputStream()
  project.exec {
    commandLine(*command)
    standardOutput = outputStream
  }
  return outputStream.toString().trim()
}
