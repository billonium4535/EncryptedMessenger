import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.encryptedmessenger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.encryptedmessenger"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    // Signing configuration
    val signingProps = Properties().apply {
        val file = rootProject.file("app/signing.properties")
        if (file.exists()) load(file.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = signingProps["RELEASE_STORE_FILE"]?.let { file(it as String) }
            storePassword = signingProps["RELEASE_STORE_PASSWORD"] as? String
            keyAlias = signingProps["RELEASE_KEY_ALIAS"] as? String
            keyPassword = signingProps["RELEASE_KEY_PASSWORD"] as? String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.bcprov.jdk15to18)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}