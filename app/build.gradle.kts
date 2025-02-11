import org.gradle.api.JavaVersion // 올바른 JavaVersion 패키지 import

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "me.moontree.treekiosk.v3"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.moontree.treekiosk.v3"
        minSdk = 21
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // 올바른 JavaVersion 참조
        targetCompatibility = JavaVersion.VERSION_17 // 올바른 JavaVersion 참조
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            storeFile = file("app/keystore.jks")
            storePassword = System.getenv("890890890")
            keyAlias = System.getenv("treekiosk")
            keyPassword = System.getenv("890890890")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // 올바른 toolchain 설정
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("io.appwrite:sdk-for-android:7.0.0")

    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("com.google.android.material:material:1.9.0")

    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
}
