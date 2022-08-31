@file:Suppress("SpellCheckingInspection")

/*
 * This file configures the build system that creates your Android app.
 * The syntax is Kotlin, not Java.
 * You do not need to understand the contents of this file, nor should you modify it.
 * Any changes will be overwritten during official grading.
 */

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.3")
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
tasks.register<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}