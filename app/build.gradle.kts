plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "pl.rosstracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pl.rosstracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.4.1"
    }

    // Keystore and passwords live in ~/.gradle/gradle.properties, never in the repo.
    val storeFilePath = providers.gradleProperty("ROSSTRACKER_STORE_FILE").orNull
    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = providers.gradleProperty("ROSSTRACKER_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("ROSSTRACKER_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("ROSSTRACKER_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
        // Release-speed build for performance tests: debug key, separate app id, includes the debug import hook.
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".staging"
            matchingFallbacks += "release"
        }
    }

    sourceSets {
        getByName("staging") {
            kotlin.directories += "src/debug/java"
            manifest.srcFile("src/debug/AndroidManifest.xml")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.junit)
}
