plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val numero = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "ec.jat.controlsuite"
    compileSdk = 35

    defaultConfig {
        applicationId = "ec.jat.controlsuite"
        minSdk = 26
        targetSdk = 34
        versionCode = numero
        versionName = "1.0.$numero"
    }

    signingConfigs {
        create("jat") {
            storeFile = file("jat.keystore")
            storePassword = "jatdatos"
            keyAlias = "jat"
            keyPassword = "jatdatos"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("jat")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("jat")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Los archivos .gz y .traineddata ya vienen comprimidos: no se vuelven a comprimir
    androidResources {
        noCompress += listOf("gz", "traineddata", "wasm", "woff2")
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    // Lector de códigos de barras de Google (ML Kit), dentro de la app: funciona sin internet
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}
