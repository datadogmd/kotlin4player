plugins{
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.kotlin4player"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kotlin4player"
        minSdk = 22
        targetSdk = 33 // Git would prefer SDK33 or higher. Was set to 22.
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

/*    kotlinOptions {
        jvmTarget = "1.8"
    }*/

    buildFeatures {
        viewBinding = true
    }
}

// Configure Kotlin compiler options outside the android block
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8) // Or JVM_11, JVM_17, etc.
    }
}


/*
// This is often the cleanest way for Android projects
kotlin {
    jvmToolchain(8) // Or 11, 17, etc.
}
*/

dependencies {
    // implementation("androidx.core:core-ktx:1.9.0")
    //implementation("androidx.appcompat:appcompat:1.7.0")
    //implementation("com.google.android.material:material:1.12.0")
    //implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //androidTestImplementation(project(":kotlin4player"))
}


/*
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.kotlin4player"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.kotlin4player"
        minSdk = 22
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}*/
