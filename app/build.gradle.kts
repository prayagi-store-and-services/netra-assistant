import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.prayagi.netraassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.prayagi.netraassistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 16
        versionName = "0.7.0-beta.1"
    }

    // Release signing: scaffold only. Reads keystore.properties if present
    // (never committed). No key exists yet; builds fall back to debug signing.
    val ksFile = rootProject.file("keystore.properties")
    val hasKey = ksFile.exists()
    if (hasKey) {
        val props = Properties().apply { ksFile.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0"
            )
        }
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
    implementation("com.google.mlkit:genai-common:1.0.0-beta3")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation("junit:junit:4.13.2")
}
