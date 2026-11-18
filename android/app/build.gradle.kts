plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.navohub.navkart"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.navohub.navkart"
        minSdk = 24
        targetSdk = 35
        versionCode = 308
        versionName = "3.0.8"

        buildConfigField(
            "String",
            "FIREBASE_RTDB_URL",
            "\"https://nav-grocery-store-default-rtdb.firebaseio.com/\""
        )
        buildConfigField(
            "String",
            "STORE_WEB_BASE",
            "\"https://www.navgrocerystore.com\""
        )
        buildConfigField("String", "STORE_UPI_ID", "\"8847370905@paytm\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Networking & Serialization for Firebase RTDB
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Indian Payment Gateway SDK (Razorpay)
    implementation("com.razorpay:checkout:1.6.40")
}
