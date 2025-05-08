buildscript {
    // gradle.properties에서 값 불러오기
    val agpVersion = project.findProperty("agpVersion") as String? ?: "8.2.0"
    val kotlinVersion = project.findProperty("kotlinVersion") as String? ?: "1.9.20"

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$agpVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

plugins {
    // plugins 블록에서는 변수를 사용할 수 없으므로 직접 버전 명시
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
