import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun configValue(key: String): String {
    val envKey = key.uppercase().replace('.', '_')
    return (localProperties.getProperty(key) ?: System.getenv(envKey) ?: "").trim()
}

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "in.campusone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.campusone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations += listOf("en", "hi")

        buildConfigField("String", "SUPABASE_URL", quoted(configValue("supabase.url")))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(configValue("supabase.anon.key")))
        buildConfigField("String", "GOOGLE_OAUTH_CLIENT_ID", quoted(configValue("google.oauth.client_id")))
        buildConfigField("String", "GOOGLE_OAUTH_WEB_CLIENT_ID", quoted(configValue("google.oauth.web_client_id")))
        buildConfigField("String", "GOOGLE_OAUTH_CALLBACK_URL", quoted(configValue("google.oauth.callback_url")))
        buildConfigField("String", "SUPABASE_EDGE_FUNCTION_URL", quoted(configValue("supabase.edge.function.url")))
        buildConfigField("String", "STORAGE_API_URL", quoted(configValue("storage.api.url")))
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
                ?: error("KEYSTORE_FILE is required for release signing")
            storeFile = file(keystorePath)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: error("KEYSTORE_PASSWORD is required for release signing")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: error("KEY_ALIAS is required for release signing")
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: error("KEY_PASSWORD is required for release signing")

            // Keep all Android APK signing schemes available for broad device compatibility.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        // Debug intentionally uses Android Gradle Plugin's standard debug signing config.
        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Material Components (Views)
    implementation(libs.material.views)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Firebase Cloud Messaging (Supabase remains the backend)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room Local DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager & Glance Widget
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // DataStore & Serialization
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("junit:junit:4.13.2")
    debugImplementation(libs.androidx.ui.tooling)
}
