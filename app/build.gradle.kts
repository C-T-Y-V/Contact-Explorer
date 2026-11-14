/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.contactexplorer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.contactexplorer"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "6.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
        xmlReport = true
        htmlReport = true
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler/reports")
    metricsDestination = layout.buildDirectory.dir("compose_compiler/metrics")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":core:api"))
    implementation(project(":core:contacts"))
    implementation(project(":core:database"))
    implementation(project(":core:permissions"))
    implementation(project(":core:plugin-api"))
    implementation(project(":core:plugin-registry"))
    implementation(project(":feature:calling"))
    implementation(project(":feature:groups"))
    implementation(project(":feature:appearance"))
    implementation(project(":feature:search"))
    implementation(project(":feature:ai"))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)

}
