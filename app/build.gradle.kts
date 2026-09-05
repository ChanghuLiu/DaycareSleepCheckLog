import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifyApplicationIdsTask : DefaultTask() {
    @get:Input
    abstract val productionApplicationId: Property<String>

    @get:Input
    abstract val debugApplicationId: Property<String>

    @get:Input
    abstract val releaseApplicationId: Property<String>

    @TaskAction
    fun verify() {
        check(productionApplicationId.get() == "com.daycare.sleepcheck.log") {
            "Unexpected production application ID: ${productionApplicationId.get()}"
        }
        check(debugApplicationId.get() == "com.daycare.sleepcheck.log.debug") {
            "Unexpected debug application ID: ${debugApplicationId.get()}"
        }
        check(releaseApplicationId.get() == "com.daycare.sleepcheck.log") {
            "Release must retain the production application ID"
        }
    }
}

private val releaseSigningPropertiesFile =
    File(System.getProperty("user.home"), ".config/daycare-sleep-check-log/signing.properties")

private val releaseOperationRequested = gradle.startParameter.taskNames.any { taskName ->
    when {
        taskName.substringAfterLast(':').contains("release", ignoreCase = true) -> true
        taskName.substringAfterLast(':').equals("assemble", ignoreCase = true) -> true
        taskName.substringAfterLast(':').equals("bundle", ignoreCase = true) -> true
        taskName.substringAfterLast(':').equals("build", ignoreCase = true) -> true
        else -> false
    }
}

private val releaseSigningProperties = if (releaseOperationRequested) {
    check(releaseSigningPropertiesFile.isFile) {
        "Release signing requires the secure file at ${releaseSigningPropertiesFile.absolutePath}"
    }
    Properties().also { properties ->
        FileInputStream(releaseSigningPropertiesFile).use(properties::load)
    }
} else {
    null
}

private fun requiredReleaseSigningProperty(name: String): String =
    releaseSigningProperties?.getProperty(name)?.takeIf { it.isNotBlank() }
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
        if (releaseOperationRequested) {
            create("release") {
                storeFile = file(requiredReleaseSigningProperty("storeFile"))
                storePassword = requiredReleaseSigningProperty("storePassword")
                keyAlias = requiredReleaseSigningProperty("keyAlias")
                keyPassword = requiredReleaseSigningProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isDebuggable = false
            if (releaseOperationRequested) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

val configuredProductionId = android.defaultConfig.applicationId
    ?: error("Production application ID must be configured")
val configuredDebugId = configuredProductionId + android.buildTypes.getByName("debug").applicationIdSuffix.orEmpty()
val configuredReleaseId = configuredProductionId + android.buildTypes.getByName("release").applicationIdSuffix.orEmpty()

tasks.register<VerifyApplicationIdsTask>("verifyApplicationIds") {
    productionApplicationId.set(configuredProductionId)
    debugApplicationId.set(configuredDebugId)
    releaseApplicationId.set(configuredReleaseId)
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
