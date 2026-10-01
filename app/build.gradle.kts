plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.equinox.vpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.equinox.vpn"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // English only — strip every other language bundled by libraries.
        resourceConfigurations += "en"
    }

    // Optional stable signing key (set by CI from repo secrets) so updates install over old versions.
    val keystorePath = System.getenv("EQUINOX_KEYSTORE")
    signingConfigs {
        if (keystorePath != null) {
            create("equinox") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("EQUINOX_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("EQUINOX_KEY_ALIAS") ?: "equinox"
                keyPassword = System.getenv("EQUINOX_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Personal app: fall back to the debug key so the APK always installs directly.
            signingConfig = signingConfigs.findByName("equinox") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Official WireGuard userspace engine.
    implementation("com.wireguard.android:tunnel:1.0.20230706")
    // QR code scanning for importing server profiles.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}
