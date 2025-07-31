plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.meeting.accesscontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.meeting.accesscontrol"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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


val camerax_version = "1.3.1"

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // 网络请求框架
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.gson)

    // Kotlin协程
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // ViewModel和LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // 核心库
    implementation("androidx.camera:camera-core:${camerax_version}")
    // Camera2 实现（推荐）
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    // 生命周期管理库
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    // 我们正在讨论的 PreviewView 所在的库
    implementation("androidx.camera:camera-view:${camerax_version}")

    // Google ML Kit 人脸检测库
    implementation("com.google.mlkit:face-detection:16.1.6")
//    implementation(libs.play.services.mlkit.face.detection)

    //动画库
    implementation("com.airbnb.android:lottie:6.1.0")
}