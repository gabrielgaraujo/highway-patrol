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
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets["main"].assets.srcDirs(
        // Share sprites and sounds with the pygame version instead of copying them.
        rootProject.file("../assets"),
    )

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
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
