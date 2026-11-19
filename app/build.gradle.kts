import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/** Upload-key settings from keystore.properties, else from RELEASE_* environment variables (CI). */
val releaseSigning: Map<String, String>?
    get() {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) {
            val p = Properties().apply { file.inputStream().use { load(it) } }
            return listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
                .associateWith { p.getProperty(it) ?: error("keystore.properties is missing $it") }
        }
        // Trimmed: values pasted into GitHub secrets easily pick up a stray space or line break.
        val env = mapOf(
            "storeFile" to System.getenv("RELEASE_STORE_FILE")?.trim(),
            "storePassword" to System.getenv("RELEASE_STORE_PASSWORD")?.trim(),
            "keyAlias" to System.getenv("RELEASE_KEY_ALIAS")?.trim(),
            "keyPassword" to System.getenv("RELEASE_KEY_PASSWORD")?.trim(),
        )
        return if (env.values.all { !it.isNullOrEmpty() }) env.mapValues { it.value!! } else null
    }

android {
    namespace = "app.kairos"
    compileSdk = 36

    defaultConfig {
        applicationId = "cc.toolcrate.kairos"
        minSdk = 26
        targetSdk = 36
        // CI numbers each build from its run (run 7 is versionCode 1007), so every upload to Play
        // is newer than the last. Builds made elsewhere use the fixed fallback.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.let { 1000 + it } ?: 1
        versionName = "0.1.0"
    }

    signingConfigs {
        // A fixed debug key, so every debug build has the same SHA-1 to register with Google
        // for Drive backup. Debug only (its password is the public "android"); never for release.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // The upload key, from keystore.properties (never committed) or CI secrets.
        // Without either, release builds are unsigned.
        releaseSigning?.let { s ->
            create("release") {
                storeFile = file(s.getValue("storeFile"))
                storePassword = s.getValue("storePassword")
                keyAlias = s.getValue("keyAlias")
                keyPassword = s.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Behaviour tests and screenshots run the real screens under Robolectric, fonts included.
        unitTests.isIncludeAndroidResources = true
    }
    lint {
        abortOnError = false
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

// Screenshots are drawn only when asked for: ./gradlew testDebugUnitTest -Pscreenshots=true
// (into app/build/screenshots). Behaviour tests in app/src/test/.../ui run every time.
tasks.withType<Test>().configureEach {
    val screenshots = providers.gradleProperty("screenshots").getOrElse("false")
    systemProperty("screenshots", screenshots)
    if (screenshots != "true") exclude("**/screenshots/**")
    maxHeapSize = "3g"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.process)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)

    implementation(libs.glance)
    implementation(libs.glance.material3)
    implementation(libs.biometric)
    implementation(libs.fragment)
    implementation(libs.splashscreen)
    implementation(libs.profileinstaller)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    implementation(libs.play.auth)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(composeBom)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.org.json)
    kspTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.test.manifest)
}
