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

fun Properties.propertyOrEnv(propertyName: String, environmentName: String): String? =
    getProperty(propertyName)?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv(environmentName)?.trim()?.takeIf { it.isNotEmpty() }

fun resolveSigningFile(rawPath: String?, label: String): java.io.File? {
    if (rawPath == null) return null
    val file = rootProject.file(rawPath)
    if (!file.exists()) {
        throw GradleException(
            "$label file not found:\n${file.absolutePath}"
        )
    }
    return file
}

val explicitStableDebugKeystorePath =
    localProperties.propertyOrEnv("stableDebug.storeFile", "SSP_STABLE_DEBUG_STORE_FILE")
val legacyStableDebugKeystoreFile =
    rootProject.file("${System.getProperty("user.home")}/.android/shift-salary-stable-debug.keystore")
val stableDebugKeystoreFile = when {
    explicitStableDebugKeystorePath != null ->
        resolveSigningFile(explicitStableDebugKeystorePath, "Stable debug signing")
    legacyStableDebugKeystoreFile.exists() -> legacyStableDebugKeystoreFile
    else -> null
}
val stableDebugStorePassword =
    localProperties.propertyOrEnv("stableDebug.storePassword", "SSP_STABLE_DEBUG_STORE_PASSWORD") ?: "android"
val stableDebugKeyAlias =
    localProperties.propertyOrEnv("stableDebug.keyAlias", "SSP_STABLE_DEBUG_KEY_ALIAS") ?: "androiddebugkey"
val stableDebugKeyPassword =
    localProperties.propertyOrEnv("stableDebug.keyPassword", "SSP_STABLE_DEBUG_KEY_PASSWORD") ?: "android"

val releaseKeystorePath =
    localProperties.propertyOrEnv("releaseSigning.storeFile", "SSP_RELEASE_STORE_FILE")
val releaseStorePassword =
    localProperties.propertyOrEnv("releaseSigning.storePassword", "SSP_RELEASE_STORE_PASSWORD")
val releaseKeyAlias =
    localProperties.propertyOrEnv("releaseSigning.keyAlias", "SSP_RELEASE_KEY_ALIAS")
val releaseKeyPassword =
    localProperties.propertyOrEnv("releaseSigning.keyPassword", "SSP_RELEASE_KEY_PASSWORD")
val releaseSigningValues = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
)
val hasAnyReleaseSigningValue = releaseSigningValues.any { it != null }
val hasCompleteReleaseSigning = releaseSigningValues.all { it != null }

if (hasAnyReleaseSigningValue && !hasCompleteReleaseSigning) {
    throw GradleException(
        "Release signing configuration is incomplete. Provide storeFile, storePassword, keyAlias and keyPassword together."
    )
}

val releaseKeystoreFile = if (hasCompleteReleaseSigning) {
    resolveSigningFile(releaseKeystorePath, "Release signing")
} else {
    null
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
        stableDebugKeystoreFile?.let { keystore ->
            create("stableDebug") {
                storeFile = keystore
                storePassword = stableDebugStorePassword
                keyAlias = stableDebugKeyAlias
                keyPassword = stableDebugKeyPassword
            }
        }
        releaseKeystoreFile?.let { keystore ->
            create("releaseProduction") {
                storeFile = keystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (stableDebugKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("stableDebug")
            }
        }
        release {
            if (releaseKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("releaseProduction")
            }
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

tasks.matching { it.name == "signingReport" }.configureEach {
    // AGP 9.2.1 SigningReportTask cannot be reliably reloaded from Gradle 9.4.1 configuration cache.
    notCompatibleWithConfigurationCache("AGP signingReport configuration-cache reload is not reliable")
}
