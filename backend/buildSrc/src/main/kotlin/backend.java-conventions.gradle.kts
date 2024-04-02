plugins {
    java
    id("io.spring.dependency-management")
    id("org.springframework.boot") apply false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation("org.springframework.boot:spring-boot")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
