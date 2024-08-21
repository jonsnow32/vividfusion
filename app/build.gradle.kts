plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.dagger.hilt)
  alias(libs.plugins.kotlin.parcelize)
}

android {
  namespace = "cloud.app.avp"
  compileSdk = 34

  defaultConfig {
    applicationId = "cloud.app.avp"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
    buildConfigField("int", "VERSION_CODE", "$versionCode")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
  viewBinding {
    enable = true
  }
  buildFeatures {
    buildConfig = true
  }
}

dependencies {

  implementation(project(":common"))
  implementation(project(":plugger"))

  //Android
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.paging.common)
  implementation(libs.androidx.paging.runtime)
  implementation(libs.androidx.swiperefreshlayout)
  //Media 3
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.cast)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.com.google.android.mediahome.video)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.datasource.okhttp)

  //Dagger
  implementation(libs.hilt)
  ksp(libs.hilt.compiler)

  //UI
  implementation(libs.material)
  implementation(libs.glide)
  ksp(libs.glide.compiler)

  //Worker
  implementation(libs.androidx.work.runtime)
  implementation(libs.androidx.work.runtime.ktx)

  //logging
  implementation(libs.timber)

  //service
  implementation(libs.tmdb)
  implementation(libs.trakt)
  implementation(libs.thetvdb)

  //network
  implementation(libs.squareup.okhttp)
  implementation(libs.squareup.okhttp.logging)
  implementation(libs.squareup.okhttp.dns)
  implementation(libs.cookiejar)

  // parse html
  implementation(libs.jsoup);

  implementation("androidx.browser:browser:1.5.0")
  implementation("jp.wasabeef:glide-transformations:4.3.0")
  implementation("me.zhanghai.android.fastscroll:library:1.3.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
}
