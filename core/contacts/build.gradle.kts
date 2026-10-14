plugins { id("com.android.library") }

android {
    namespace = "com.example.contactexplorer.core.contacts"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }

dependencies {
    api(project(":core:api"))
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
}
