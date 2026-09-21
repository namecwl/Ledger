plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseStoreFile = System.getenv("LEDGER_KEYSTORE_FILE")
val releaseStorePassword = System.getenv("LEDGER_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("LEDGER_KEY_ALIAS")
val releaseKeyPassword = System.getenv("LEDGER_KEY_PASSWORD")

// 随仓库提交的固定签名密钥，保证每次 CI 构建签名一致、新版本可直接覆盖安装
val fixedKeystore = file("keystore/ledger.jks")
val fixedStorePassword = "ledger123"
val fixedKeyAlias = "ledger"
val fixedKeyPassword = "ledger123"

android {
    namespace = "com.ledger.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ledger.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.5.1"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (fixedKeystore.exists()) {
            create("fixed") {
                storeFile = fixedKeystore
                storePassword = fixedStorePassword
                keyAlias = fixedKeyAlias
                keyPassword = fixedKeyPassword
            }
        }
        if (!releaseStoreFile.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // debug 也用固定签名，避免 debug/performance 互换或升级时签名冲突
            signingConfigs.findByName("fixed")?.let { signingConfig = it }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("fixed")
                ?: signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("performance") {
            initWith(getByName("debug"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("fixed")
                ?: signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}