import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Load local signing config from keystore.properties (git-ignored) if present.
// CI has no such file and supplies the same values via environment variables
// (RELEASE_* — set from GitHub secrets in the workflow).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

fun signingValue(envName: String, propName: String): String? =
    System.getenv(envName) ?: keystoreProps.getProperty(propName)

android {
    namespace = "com.jwilgeroth.sleeptimertile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jwilgeroth.sleeptimertile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("RELEASE_STORE_FILE", "storeFile")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("RELEASE_STORE_PASSWORD", "storePassword")
                keyAlias = signingValue("RELEASE_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // Only attach the release signing config if we actually have a
            // keystore (locally or via CI env). Otherwise release builds stay
            // unsigned rather than failing configuration.
            signingConfig = if (signingValue("RELEASE_STORE_FILE", "storeFile") != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}