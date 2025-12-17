buildscript {
    val kotlin_version by extra("2.2.0")
    dependencies {
        classpath("com.google.gms:google-services:4.4.4")
        classpath ("com.android.tools.build:gradle:8.11.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.11.2" apply false
}