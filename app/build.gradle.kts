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

        To keep one SHA-1 on all Android Studio builds, copy the same keystore file
        and set stableDebug.storeFile in local.properties.
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
        minSdk = 27
        targetSdk = 36
        versionCode = 67
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

tasks.register<Exec>("printSigningSha1") {
    group = "verification"
    description = "Prints SHA-1 fingerprint for the stable Android Studio signing key."
    commandLine(
        "keytool",
        "-list",
        "-v",
        "-keystore", stableDebugKeystoreFile.absolutePath,
        "-alias", stableDebugKeyAlias,
        "-storepass", stableDebugStorePassword,
        "-keypass", stableDebugKeyPassword
    )
}
