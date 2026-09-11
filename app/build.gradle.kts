import java.util.Properties
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
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
        minSdk = 27
        targetSdk = 36
        versionCode = 201
        versionName = "7.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
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
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Apache POI (Excel)
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Google Sign-In + Drive AppData sync
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)

    // Wear OS companion sync
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
}

tasks.register("printSigningSha1") {
    group = "verification"
    description = "Prints SHA-1 for explicitly configured stable debug signing, if present."
    doLast {
        val keystore = stableDebugKeystoreFile
        if (keystore == null) {
            logger.lifecycle(
                "No stable debug keystore is configured; Android default debug signing is active. " +
                    "Run :app:signingReport to inspect its certificate."
            )
            return@doLast
        }

        val process = ProcessBuilder(
            "keytool",
            "-list",
            "-v",
            "-keystore", keystore.absolutePath,
            "-alias", stableDebugKeyAlias,
            "-storepass", stableDebugStorePassword,
            "-keypass", stableDebugKeyPassword
        ).inheritIO().start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("keytool failed with exit code $exitCode")
        }
    }
}

tasks.matching { it.name == "signingReport" }.configureEach {
    // AGP 9.2.1 SigningReportTask cannot be reliably reloaded from Gradle 9.4.1 configuration cache.
    notCompatibleWithConfigurationCache("AGP signingReport configuration-cache reload is not reliable")
}
