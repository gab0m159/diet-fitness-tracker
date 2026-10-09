plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.diettracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.diettracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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

    // Keep the debug signing keystore inside the build directory instead of the
    // default ~/.android/debug.keystore. This keeps the build self-contained and
    // avoids permission problems in restricted/sandboxed environments.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("build/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
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
    }

    composeOptions {
        // Kotlin 1.9.24 -> Compose Compiler 1.5.14
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room writes the generated schema to app/schemas/<db-class>/<version>.json.
// We keep it in the repo so the hand-written migration can use the exact DDL Room
// expects (any drift here shows up as "Migration didn't properly handle ..."),
// and so future schema changes have a reference to diff against.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Core / lifecycle
    // core-ktx brings androidx.core:core in transitively; explicitly required for
    // WindowCompat / WindowInsetsControllerCompat used by the theme and scaffolds.
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // Compose UI + Material 3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // 纯逻辑单元测试（进度规则 / 频率排程），不需要设备。
    testImplementation("junit:junit:4.13.2")
}
