plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
    `java-library`
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
    // Kotlin Coroutines для асинхронности
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")
    
    // JSON сериализация
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
    
    // HTTP клиент для API взаимодействия
    implementation("io.ktor:ktor-client-core:3.3.1")
    implementation("io.ktor:ktor-client-cio:3.3.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")
    
    // HTTP сервер для API
    implementation("io.ktor:ktor-server-core:3.3.1")
    implementation("io.ktor:ktor-server-netty:3.3.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.1")
    implementation("io.ktor:ktor-server-cors:3.3.1")
    implementation("io.ktor:ktor-server-websockets:3.3.1")
    
    // Vector Database для семантического поиска
    implementation("org.apache.lucene:lucene-core:9.9.2")
    implementation("org.apache.lucene:lucene-analysis-common:9.9.2")
    implementation("org.apache.lucene:lucene-queryparser:9.9.2")
    
    // Embedding generation
    implementation("com.microsoft.onnxruntime:onnxruntime:1.21.1")
    
    // Криптография для безопасности
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    
    // Логирование
    api("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Конфигурация
    implementation("com.typesafe:config:1.4.3")
    
    // Процессы и система
    implementation("org.apache.commons:commons-exec:1.4.0")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.ktor:ktor-server-test-host:3.3.1")
}

tasks.test {
    useJUnitPlatform()
}