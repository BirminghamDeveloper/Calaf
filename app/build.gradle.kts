plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.calaf.app"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.calaf.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true; buildConfig = true }
    flavorDimensions += "backend"
    productFlavors {
        // Fully offline showcase: no backend, no INTERNET permission, no Firebase.
        create("demo") {
            dimension = "backend"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
        }
        // The real app on Firebase. Emulators are only ever used by debug builds of this flavour.
        create("live") {
            dimension = "backend"
            val emulators = providers.gradleProperty("calaf.emulators").orElse("false").get().toBoolean()
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", emulators.toString())
        }
    }
    buildTypes {
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
    implementation(project(":feature:community"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.exif)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Firebase and its data layer exist only in the live flavour. The demo APK never contains them.
    "liveImplementation"(project(":core:data"))
    "liveImplementation"(platform(libs.firebase.bom))
    "liveImplementation"(libs.firebase.auth)
    "liveImplementation"(libs.firebase.storage)
    "liveImplementation"(libs.coroutines.tasks)
    "liveImplementation"(libs.datastore)
    coreLibraryDesugaring(libs.desugar)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.test.runner)
    debugImplementation(libs.compose.test.manifest)
}

// The demo flavour never initialises or connects to Firebase, and its `.demo` package has no Firebase client.
androidComponents {
    onVariants(selector().withFlavor("backend" to "demo")) { variant ->
        val taskName = "process${variant.name.replaceFirstChar { it.uppercase() }}GoogleServices"
        tasks.configureEach { if (name == taskName) enabled = false }
    }
}
