import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.compose")
    id("kotlin-parcelize")
    id("android-build-flavours")
}

android {
    namespace = "com.zhenxiang.superimage"
    compileSdk = 33

    val changelogFileName = "changelog.txt"

    defaultConfig {
        applicationId = "com.zhenxiang.superimage"
        minSdk = 24
        targetSdk = 33
        versionCode = 132
        versionName = "1.3.2"

        buildConfigField("String", "CHANGELOG_ASSET_NAME", "\"$changelogFileName\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Copy changelog for app assets
    val copiedChangelogPath = File(buildDir, "generated/changelogAsset")
    val copyArtifactsTask = tasks.register<Copy>("copyChangelog") {
        delete(copiedChangelogPath)
        from(File(rootProject.rootDir, "fastlane/metadata/android/en-US/changelogs/${defaultConfig.versionCode}.txt"))
        into(copiedChangelogPath)
        rename { changelogFileName }
    }
    tasks.preBuild {
        dependsOn(copyArtifactsTask)
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    sourceSets {
        getByName("main") {
            // Add changelog to assets
            assets.srcDirs(copiedChangelogPath)
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

@OptIn(ExperimentalComposeLibrary::class)
dependencies {

    implementation(project(":decompose"))
    "freeImplementation"(project(":playstore:no-op"))
    "playstoreImplementation"(project(":playstore:impl"))
    implementation(project(":realesrgan:android"))
    implementation(project(":shared"))

    val koin_android_version= "3.3.2"

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.lifecycle:lifecycle-process:2.6.0-beta01")
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.preview)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("com.github.Dimezis:BlurView:version-2.0.3")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.29.1-alpha")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation("io.insert-koin:koin-android:$koin_android_version")
    implementation("joda-time:joda-time:2.12.2")
    implementation("org.apache.commons:commons-imaging:1.0-alpha3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.6.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(compose.uiTestJUnit4)
    debugImplementation(compose.uiTooling)
}