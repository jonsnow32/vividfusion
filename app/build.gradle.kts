plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.dagger.hilt)
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
}

dependencies {

  //Testing
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)

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

  //Worker
  implementation(libs.androidx.work.runtime)
  implementation(libs.androidx.work.runtime.ktx)

  //logging
  implementation(libs.timber)

  //service
  implementation(libs.tmdb)
  implementation(libs.trakt)
  implementation(libs.thetvdb)
}
