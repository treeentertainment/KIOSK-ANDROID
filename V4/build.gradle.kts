buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Android Gradle Plugin 버전 명시
        classpath("com.android.tools.build:gradle:8.2.0")
        // Kotlin Gradle Plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")

        // 필요 시 추가적인 classpath 의존성
        // classpath("com.google.dagger:hilt-android-gradle-plugin:2.50")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
