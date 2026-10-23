plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vintagecam.core.camera"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:gl-pipeline"))
    implementation(project(":domain:presets"))
    implementation(project(":core:common"))

    val cameraxVersion = "1.3.4"
    api("androidx.camera:camera-core:$cameraxVersion")
    api("androidx.camera:camera-camera2:$cameraxVersion")
    api("androidx.camera:camera-lifecycle:$cameraxVersion")
    api("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
}
