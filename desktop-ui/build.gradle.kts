plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.0"
}

group = "com.github.lonmstalker.aiintegration"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // Core module dependency
    implementation(project(":core"))
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.9")
}

tasks.test {
    useJUnitPlatform()
}

// TODO: Add Compose Desktop/Electron configuration for Desktop UI