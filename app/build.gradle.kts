plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}
android {
    namespace = "com.calaf.app"
    testBuildType = providers.gradleProperty("calaf.testBuildType").orElse("debug").get()
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.calaf.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "OFFLINE_DEMO", "false")
        buildConfigField("boolean", "USE_FIREBASE_EMULATORS", providers.gradleProperty("calaf.emulators").orElse("false").get().toBoolean().toString())
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        create("offline") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".offline"
            versionNameSuffix = "-offline-demo"
            matchingFallbacks += "debug"
            buildConfigField("boolean", "OFFLINE_DEMO", "true")
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:community"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.exif)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.tasks)
    implementation(libs.datastore)
    coreLibraryDesugaring(libs.desugar)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.test.runner)
    debugImplementation(libs.compose.test.manifest)
}

// The standalone demo never initializes or connects to Firebase.
tasks.configureEach { if (name == "processOfflineGoogleServices") enabled = false }
