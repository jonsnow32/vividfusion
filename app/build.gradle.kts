import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.dagger.hilt)
  kotlin("plugin.serialization")
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics.plugin)
}

val keyProps = Properties().also { props ->
  val keyFile = rootProject.file("key.properties")
  if (keyFile.exists()) props.load(FileInputStream(keyFile))
}

// AdMob IDs are resolved (in order) from: a gradle -P property (injected by Fastlane/CI),
// local.properties (not committed), then an env var, then Google's official test IDs.
// Release builds use the real IDs when supplied; debug builds always use test IDs
// (clicking real ads in debug violates AdMob policy).
val localProps = Properties().also { props ->
  val localFile = rootProject.file("local.properties")
  if (localFile.exists()) props.load(FileInputStream(localFile))
}
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerId = "ca-app-pub-3940256099942544/6300978111"
val testAdmobInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testAdmobRewardedId = "ca-app-pub-3940256099942544/5224354917"
fun admobId(key: String, fallback: String): String =
  (findProperty(key) as String?) ?: localProps.getProperty(key) ?: System.getenv(key) ?: fallback

val releaseAdmobAppId = admobId("ADMOB_APP_ID", testAdmobAppId)
val releaseAdmobBannerId = admobId("ADMOB_BANNER_AD_UNIT_ID", testAdmobBannerId)
val releaseAdmobInterstitialId = admobId("ADMOB_INTERSTITIAL_AD_UNIT_ID", testAdmobInterstitialId)
val releaseAdmobRewardedId = admobId("ADMOB_REWARDED_AD_UNIT_ID", testAdmobRewardedId)

android {
  namespace = "cloud.app.vvf"
  compileSdk = 35

  defaultConfig {
    applicationId = "cloud.app.vvf"
    minSdk = 24
    targetSdk = 35
    versionCode = 109
    versionName = "1.0.9"
    buildConfigField("int", "VERSION_CODE", "$versionCode")
    buildConfigField("String", "AUTHORITY_FILE_PROVIDER", "\"${applicationId}.fileprovider\"")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    multiDexEnabled = true

    // Default to AdMob test IDs (used by debug and any build without real IDs).
    manifestPlaceholders["admobAppId"] = testAdmobAppId
    buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$testAdmobBannerId\"")
    buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$testAdmobInterstitialId\"")
    buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$testAdmobRewardedId\"")
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
  }

  signingConfigs {
    create("release") {
      if (keyProps.isNotEmpty()) {
        storeFile = file(keyProps["storeFile"] as String)
        storePassword = keyProps["storePassword"] as String
        keyAlias = keyProps["keyAlias"] as String
        keyPassword = keyProps["keyPassword"] as String
        storeType = keyProps.getProperty("storeType", "PKCS12")
      }
    }
  }

  buildTypes {
    release {
      if (keyProps.isNotEmpty()) {
        signingConfig = signingConfigs.getByName("release")
      }
      isMinifyEnabled = true
      isShrinkResources = true
      isDebuggable = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )

      // Real AdMob IDs (gradle property / local.properties / env var); test IDs otherwise.
      manifestPlaceholders["admobAppId"] = releaseAdmobAppId
      buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$releaseAdmobBannerId\"")
      buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$releaseAdmobInterstitialId\"")
      buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$releaseAdmobRewardedId\"")
    }
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    jvmToolchain(17)
  }

  lint {
    lintConfig = file("lint.xml")
    abortOnError = true
    warningsAsErrors = false
    checkReleaseBuilds = false
  }

  viewBinding {
    enable = true
  }
  buildFeatures {
    buildConfig = true
  }
}

dependencies {

  coreLibraryDesugaring(libs.android.desugar.jdk.libs)

  implementation(project(":common"))
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
  implementation(libs.kotlinx.coroutines.guava)

  //Media 3
  implementation(libs.bundles.media3)
  implementation(libs.mediahome.video)
  // FFmpeg Decoding
  implementation(libs.bundles.nextlibMedia3)
  implementation(libs.juniversalchardet)
  //Dagger
  implementation(libs.hilt)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.work)
  ksp(libs.androidx.hilt.compiler)

  // Torrent Support
  implementation(libs.torrentserver)

  //UI
  implementation(libs.material)
  implementation(libs.glide)
  ksp(libs.glide.compiler)
  implementation(libs.nestedscrollwebview)
  implementation(libs.pikolo)

  //Worker
  implementation(libs.androidx.work.runtime)
  implementation(libs.androidx.work.runtime.ktx)

  //logging
  implementation(libs.timber)

  // Retrofit2 + Gson (used by debrid/torrent API clients)
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.google.code.gson:gson:2.10.1")

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

  // Ads
  implementation(libs.admob)
  implementation(libs.facebook.audience.network)
  implementation(libs.ironsource.mediationsdk)
  implementation(libs.applovin)
  implementation(libs.unity.ads)
  implementation(libs.vungle)

  // Firebase - BOM for version management
  implementation(platform(libs.firebase.bom))
  implementation(libs.bundles.firebase.core)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)

}
