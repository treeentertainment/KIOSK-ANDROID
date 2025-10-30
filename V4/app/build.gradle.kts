plugins {
    id("com.android.application") version "8.13.0" // Android Gradle Plugin 최신 안정 버전
    id("org.jetbrains.kotlin.android") version "2.2.21" // Kotlin 최신 안정 버전
    id("com.google.gms.google-services") version "4.4.4" // 그대로 사용 가능
    id("org.jetbrains.kotlin.plugin.compose") // ✅ Compose Compiler 플러그인 추가
}

android {
    namespace = "tech.treeentertainment.treekiosk.v4"
    compileSdk = 36

    defaultConfig {
        applicationId = "tech.treeentertainment.treekiosk.v4"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "4.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // 최신 Compose와 Firebase 권장값
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // AndroidX
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.11.0")

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Firebase (최신 BOM 사용)
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")      // = 
    implementation("com.google.firebase:firebase-database")  // = 


    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
