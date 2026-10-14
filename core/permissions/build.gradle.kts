plugins { id("com.android.library") }

android {
    namespace = "com.example.contactexplorer.core.permissions"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }

dependencies { implementation("androidx.core:core-ktx:1.18.0") }
