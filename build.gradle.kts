// 프로젝트 속성(properties)에서 값 불러오기
val agpVersion: String by project
val kotlinVersion: String by project

buildscript {
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
    id("com.android.application") version agpVersion apply false
    id("com.android.library") version agpVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
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
