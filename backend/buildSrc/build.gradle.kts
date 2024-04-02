plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.spring.gradle:dependency-management-plugin:1.1.4")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.2.3")
}
