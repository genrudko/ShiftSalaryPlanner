import java.util.Properties
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val stableDebugKeystorePath =
    localProperties.getProperty("stableDebug.storeFile")
        ?: "${System.getProperty("user.home")}/.android/shift-salary-stable-debug.keystore"
val stableDebugKeystoreFile = rootProject.file(stableDebugKeystorePath)
val stableDebugStorePassword =
    localProperties.getProperty("stableDebug.storePassword") ?: "android"
val stableDebugKeyAlias =
    localProperties.getProperty("stableDebug.keyAlias") ?: "androiddebugkey"
val stableDebugKeyPassword =
    localProperties.getProperty("stableDebug.keyPassword") ?: "android"

if (!stableDebugKeystoreFile.exists()) {
    throw GradleException(
        """
        Stable signing keystore not found:
        ${stableDebugKeystoreFile.absolutePath}

        Wear Data Layer requires phone and watch APKs to use the same app id and signing key.
        Copy the same keystore file and set stableDebug.storeFile in local.properties.
        """.trimIndent()
    )
}

android {
    namespace = "com.vigilante.shiftsalaryplanner"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.vigilante.shiftsalaryplanner"
        minSdk = 30
        targetSdk = 36
        versionCode = 360560002
        versionName = "5.7-wear"
    }

    signingConfigs {
        create("stableDebug") {
            storeFile = stableDebugKeystoreFile
            storePassword = stableDebugStorePassword
            keyAlias = stableDebugKeyAlias
            keyPassword = stableDebugKeyPassword
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
        release {
            signingConfig = signingConfigs.getByName("stableDebug")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.ui.tooling)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.tiles.material)
    implementation(libs.androidx.wear.watchface.complications.data.source.ktx)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.guava)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
