plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.gabrielgaraujo.highwaypatrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.gabrielgaraujo.highwaypatrol"
        minSdk = 24
        targetSdk = 35
        // CI numbers each build so a new APK always installs as an update.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "1.0.$build"
    }

    signingConfigs {
        getByName("debug") {
            // Fixed debug key so every CI build can install over the previous one.
            // Debug/testing only: never use this key for a Play Store release.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // The release key is private: CI decodes it from the SIGNING_KEYSTORE_BASE64
        // secret and passes its path and password in these environment variables.
        val releaseKeystore = System.getenv("SIGNING_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SIGNING_PASSWORD")
                keyAlias = "highwaypatrol"
                keyPassword = System.getenv("SIGNING_PASSWORD")
            }
        }
    }

    sourceSets["main"].assets.srcDirs(
        // Share sprites and sounds with the pygame version instead of copying them.
        rootProject.file("../assets"),
    )

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Without the key (local builds, forks) this produces an unsigned APK.
            signingConfig = signingConfigs.findByName("release")
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

dependencies {
    testImplementation("junit:junit:4.13.2")
}
