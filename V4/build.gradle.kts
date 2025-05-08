// Root-level build.gradle.kts

plugins {
    // 플러그인은 여기에 작성하지 않음 (보통 module-level에 사용)
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        // classpath("com.google.dagger:hilt-android-gradle-plugin:2.50")
    }

    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
