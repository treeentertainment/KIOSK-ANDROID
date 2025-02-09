plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "me.moontree.treekiosk.v3"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.moontree.treekiosk.v3"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("io.appwrite:sdk-for-android:4.0.0")
}
