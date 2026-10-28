plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.srmatta.treino"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.srmatta.treino"
        minSdk = 23          // Android 6.0 ou superior (o J7 Prime 2 roda 7.1 / 8.1)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // R8 encolhe o código e os recursos: o APK final fica pequeno.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Assina com a chave de debug para o APK ser instalável direto no celular.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Sem nenhuma biblioteca extra (nada de AndroidX): só o framework do Android.
