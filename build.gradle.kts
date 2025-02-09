// build.gradle.kts (Project-level)

plugins {
    // If you have any plugins here, keep them.  Otherwise, this block can be omitted.
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val kotlinVersion = "1.9.10" // Or latest version

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2") // Or latest
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}") { // Correct way
            // You can optionally add configuration here if needed
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
