import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.myonepacelist"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.myonepacelist"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        // Catálogo remoto que regenera la GitHub Action semanal (ver .github/workflows/catalog.yml).
        buildConfigField(
            "String",
            "CATALOG_URL",
            "\"https://raw.githubusercontent.com/${providers.gradleProperty("githubUser").get()}/MyOnePaceList/main/catalog.json\"",
        )
    }

    // La clave de firma vive en `signing/` (fuera de git). En CI llega por variables de entorno.
    val keystoreFile = rootProject.file("signing/keystore.properties")
    val keystoreProps = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }
    fun prop(key: String, env: String) = keystoreProps.getProperty(key) ?: System.getenv(env)

    signingConfigs {
        val storePath = prop("storeFile", "SIGNING_STORE_FILE")
        if (storePath != null) {
            create("release") {
                storeFile = rootProject.file(if (keystoreFile.exists()) "signing/$storePath" else storePath)
                storePassword = prop("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = prop("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = prop("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.work)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
