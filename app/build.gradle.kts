plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // No kapt needed now since Room is POE-only; add kapt later if you include Room.
}

android {
    namespace = "za.co.studysync"
    compileSdk = 35

    defaultConfig {
        applicationId = "za.co.studysync"
        minSdk = 29
        targetSdk = 35
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

    // Use Java 17 to match modern toolchains and CI
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Optional but handy
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Your existing version-catalog dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ---- Added: Lifecycle / ViewModel ----
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // ---- Added: Navigation (Fragments + BottomNav) ----
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ---- Added: Settings (DataStore) ----
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ---- Added: Network (Retrofit + OkHttp + Moshi) ----
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ---- Added: Google SSO (Play Services Auth) ----
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // (Optional to keep; not required for classic sign-in)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ---- Added: Logging ----
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // Tests (kept)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
