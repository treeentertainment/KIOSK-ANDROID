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

    // 최신 Appwrite SDK 사용
    implementation("io.appwrite:sdk-for-android:4.0.1") // 최신 버전 확인 필요
    
    // 추가된 의존성 최신화
    implementation("androidx.fragment:fragment-ktx:1.6.2")  // 최신 버전으로 변경
    implementation("androidx.recyclerview:recyclerview:1.3.1") // 최신화

    // ViewPager 및 CoordinatorLayout 최신화
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
    implementation("com.squareup.okhttp3:okhttp")
    // Legacy 지원 제거 (가능하면 사용 X)
    implementation("androidx.legacy:legacy-support-core-utils:1.0.0") // 제거 고려
}
