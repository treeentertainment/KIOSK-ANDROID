buildscript {
    repositories {
        google()  // ✅ Android Gradle Plugin 저장소
        mavenCentral()  // ✅ Kotlin 플러그인 저장소
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

