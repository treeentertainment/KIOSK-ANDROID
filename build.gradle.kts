plugins {
    id("com.android.application") version "8.2.0" apply false  // Correct syntax
    id("com.android.library") version "8.2.0" apply false      // Correct syntax
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false // Correct syntax (and updated Kotlin version)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        val kotlinVersion: String by project // Get from gradle.properties
        val agpVersion: String by project      // Get from gradle.properties

        classpath("com.android.tools.build:gradle:$agpVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
