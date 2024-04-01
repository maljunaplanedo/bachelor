plugins {
    java
    id("backend.java-conventions")
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "ru.dbhub"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation(project(":lib:article"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
