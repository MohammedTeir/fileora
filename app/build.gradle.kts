plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing comes ONLY from environment variables (set by the release workflow from
// GitHub secrets). Nothing secret is ever stored in the repository.
val signingKeystorePath: String? = providers.environmentVariable("SIGNING_KEYSTORE_PATH").orNull

// Resolve the Robolectric SDK through Gradle so tests work with a read-only home
// and use the same dependency cache/repositories as the rest of the build.
val robolectricRuntime by configurations.creating
val robolectricResources by configurations.creating
val prepareRobolectricRuntime by tasks.registering(Sync::class) {
    from(robolectricRuntime, robolectricResources)
    into(layout.buildDirectory.dir("robolectric-runtime"))
}

android {
    namespace = "dev.deviceagent.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.deviceagent.app"
        minSdk = 26 // java.nio.file (used by agent-core's JvmFileSystem) needs API 26
        targetSdk = 35
        // CI passes -PversionCode=<run number> and -PversionName=<tag>; Android requires versionCode to only ever increase.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
    }

    signingConfigs {
        if (signingKeystorePath != null) {
            create("release") {
                storeFile = file(signingKeystorePath)
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
            }
        }
    }

    // One APK per CPU architecture plus a universal one. The app is pure Kotlin today, so these
    // are functionally identical until native libraries (FFmpeg, proot, ...) are added; the
    // pipeline is ready for that.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingKeystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.dependsOn(prepareRobolectricRuntime)
            it.systemProperty("robolectric.offline", "true")
            it.systemProperty("robolectric.dependency.dir", layout.buildDirectory.dir("robolectric-runtime").get().asFile.absolutePath)
        }
    }
}

dependencies {
    implementation(project(":agent-core"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    robolectricRuntime("org.robolectric:android-all-instrumented:14-robolectric-10818077-i7")
    robolectricResources("org.robolectric:android-all-instrumented:15-robolectric-12650502-i7")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
