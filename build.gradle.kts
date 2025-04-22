// Top-level build file where you can add configuration options common to all sub-projects/modules.
/*plugins {
    id("com.android.application") version "8.9.1" apply false
    //id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    //kotlin("android") version "2.1.0"
}*/
// Top-level build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2") // Android Gradle Plugin (AGP)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22") // Kotlin Gradle Plugin
    }
}

plugins {
    // Apply KSP if needed (recommended for Room)
    kotlin("jvm") version "1.9.22" apply false
}