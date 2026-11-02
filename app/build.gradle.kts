import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

// The fixed signing key is kept base64-encoded in the repo
// (signing/dayblock.keystore.b64) and decoded to a real keystore
// file here, before the signing config below uses it.
val dayblockKeystore = File(rootProject.projectDir, "build/dayblock.keystore").apply {
    parentFile.mkdirs()
    writeBytes(
        Base64.getDecoder().decode(
            rootProject.file("signing/dayblock.keystore.b64").readText().trim()
        )
    )
}

android {
    namespace = "com.farhan.dayblock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.farhan.dayblock"
        minSdk = 26
        targetSdk = 35
        // GitHub Actions builds get a fresh, higher version code from the
        // workflow run number, so each new APK installs as an update.
        // Local builds keep version code 1.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.plus(1000)) ?: 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // One fixed development key for every build, so the app signature
    // never changes and new APKs install over the old app as updates.
    // The key lives in this private repo (signing/dayblock.keystore.b64)
    // by design; if Dayblock is ever published, switch to a secret release
    // key stored outside the repo first.
    signingConfigs {
        create("dayblock") {
            storeFile = dayblockKeystore
            storePassword = "dayblock"
            keyAlias = "dayblock"
            keyPassword = "dayblock"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dayblock")
        }
        release {
            signingConfig = signingConfigs.getByName("dayblock")
            isMinifyEnabled = false
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
