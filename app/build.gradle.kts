plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.dagger.hilt)
  kotlin("plugin.serialization")
}

android {
  namespace = "cloud.app.vvf"
  compileSdk = 35

  defaultConfig {
    applicationId = "cloud.app.vvf"
    minSdk = 24
    targetSdk = 35
    versionCode = 100
    versionName = "1.0.0"
    buildConfigField("int", "VERSION_CODE", "$versionCode")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    jvmToolchain(17)
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
  implementation(libs.plugger)
  //Android
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.fragment.ktx)
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
  implementation(libs.nestedscrollwebview)

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

  implementation(libs.androidx.tvprovider)
  implementation(libs.androidx.browser)
  implementation(libs.glide.transformations)
  implementation(libs.fastscroll.library)

  implementation(libs.pikolo)
  implementation(kotlin("reflect"))

  implementation("androidx.hilt:hilt-work:1.2.0")
  ksp("androidx.hilt:hilt-compiler:1.2.0")
}
