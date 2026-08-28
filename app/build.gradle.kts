import java.io.FileInputStream
import java.util.Properties

private val releaseSigningPropertiesFile =
    File(System.getProperty("user.home"), ".config/daycare-sleep-check-log/signing.properties")

check(releaseSigningPropertiesFile.isFile) {
    "Release signing requires the secure file at ${releaseSigningPropertiesFile.absolutePath}"
}

private val releaseSigningProperties = Properties().also { properties ->
    FileInputStream(releaseSigningPropertiesFile).use(properties::load)
}

private fun requiredReleaseSigningProperty(name: String): String =
    releaseSigningProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: error("Missing release signing property: $name")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.daycare.sleepcheck.log"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.daycare.sleepcheck.log"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = file(requiredReleaseSigningProperty("storeFile"))
            storePassword = requiredReleaseSigningProperty("storePassword")
            keyAlias = requiredReleaseSigningProperty("keyAlias")
            keyPassword = requiredReleaseSigningProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.google.play.billing.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
