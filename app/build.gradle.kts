@file:Suppress("SpellCheckingInspection", "MagicNumber", "GradleDependency")

import java.util.function.BiConsumer
import java.util.UUID

/*
 * This file configures the build system that creates your Android app.
 * The syntax is Kotlin, not Java.
 * You do not need to understand the contents of this file, nor should you modify it.
 * Any changes will be overwritten during official grading.
 */

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}
plugins {
    id("com.android.application")
    id("com.github.cs125-illinois.gradlegrader") version "2021.10.10"
    checkstyle
    id("com.github.sherter.google-java-format") version "0.9"
}
android {
    compileSdk = 31
    buildToolsVersion = "30.0.2"
    lint {
        disable("OldTargetApi", "GradleDependency")
    }
    defaultConfig {
        applicationId = "edu.illinois.cs.cs124.ay2021.eatable"
        minSdk = 24
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}
dependencies {
    /*
     * Do not add dependencies here, since they will be overwritten during official grading.
     * If you have a package that you think would be broadly useful for completing the MP, please start a discussion
     * on the forum.
     */
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("com.github.cs124-illinois.ListAdapters:sorted-list-adapter:2021.7.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.squareup.okhttp3:mockwebserver:4.9.2")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.3.1")
    implementation("androidx.test.espresso:espresso-idling-resource:3.4.0")
    implementation("com.android.support:support-annotations:28.0.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.opencsv:opencsv:5.5.2")
    implementation("org.apache.commons:commons-text:1.9")

    testImplementation("com.github.cs125-illinois:gradlegrader:2021.10.10")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.6.1")
    testImplementation("org.robolectric:shadows-httpclient:4.6.1")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("androidx.test.ext:junit:1.1.3")
    testImplementation("androidx.test.ext:truth:1.4.0")
    testImplementation("androidx.test.espresso:espresso-core:3.4.0")
    testImplementation("androidx.test.espresso:espresso-intents:3.4.0")
    testImplementation("androidx.test.espresso:espresso-contrib:3.4.0")
}
googleJavaFormat {
    toolVersion = "1.7"
}
checkstyle {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    toolVersion = "9.0"
}
tasks.register("checkstyle", Checkstyle::class) {
    source("src/main/java")
    include("**/*.java")
    classpath = files()
}
gradlegrader {
    assignment = "AY2021.MP"
    checkpoint {
        yamlFile = rootProject.file("grade.yaml")
        configureTests(
                BiConsumer { MP, test ->
                    require(MP in setOf("0", "1", "2", "3")) { "Cannot grade unknown checkpoint MP$MP" }
                    test.setTestNameIncludePatterns(listOf("MP${MP}Test"))
                    test.filter.isFailOnNoMatchingTests = false
                }
        )
    }
    checkstyle {
        points = 10
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        version = "9.0"
    }
    forceClean = false
    identification {
        txtFile = rootProject.file("ID.txt")
        validate = Spec {
            try {
                UUID.fromString(it.trim())
                true
            } catch (e: java.lang.IllegalArgumentException) {
                false
            }
        }
    }
    reporting {
        post {
            endpoint = "https://cloud.cs124.org/gradlegrader"
        }
        printPretty {
            title = "Grade Summary"
            notes = "On checkpoints with an early deadline, the maximum local score is 90/100. " +
                    "10 points will be provided during official grading if you submit code " +
                    "that meets the early deadline threshold before the early deadline."
        }
    }
    vcs {
        git = true
        requireCommit = true
    }
}
