/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java library project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.4/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("maven-publish")

    id("org.springframework.boot") version "3.2.7" // Replace with your desired version
    id("io.spring.dependency-management") version "1.1.3" // Dependency management plugin
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    mavenLocal()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    api("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    api("io.projectreactor:reactor-test:3.6.5")
    api("software.amazon.awssdk:s3:2.17.195")
    api("software.amazon.awssdk:netty-nio-client:2.17.195")
    api("net.coobird:thumbnailator:0.4.14")
    api("org.bytedeco:javacv-platform:1.5.7")
    api("com.madgag:animated-gif-lib:1.4")

    testImplementation("junit:junit:4.13.2")
    implementation("org.projectlombok:lombok:1.18.20")

}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/sonamsamdupkhangsar/sonam-s3-lib")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("PERSONAL_ACCESS_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "cloud.sonam"
            artifactId = "sonam-s3-lib"
            version = "1.0.0-SNAPSHOT"
        }
    }
}

tasks.bootJar {
    enabled = false
    //archiveFileName.set("my-library.jar")
}