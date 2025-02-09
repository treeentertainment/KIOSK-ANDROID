import org.gradle.api.initialization.resolve.RepositoriesMode

// gradle.properties에서 값 불러오기
val agpVersion = project.properties["agpVersion"] as String? ?: "8.2.0"
val kotlinVersion = project.properties["kotlinVersion"] as String? ?: "1.9.20"

plugins {
    id("com.android.application") version agpVersion apply false
    id("com.android.library") version agpVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
}

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

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
