plugins {
    id("java-library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {

    val decompose_version = "1.0.0"

    api("com.arkivanov.decompose:decompose:$decompose_version")
    api("com.arkivanov.decompose:extensions-compose-jetbrains:$decompose_version")
    implementation(compose.runtime)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}
