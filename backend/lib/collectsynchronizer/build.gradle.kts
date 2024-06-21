plugins {
    id("backend.lib-conventions")
}

group = "ru.dbhub"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("org.hibernate:hibernate-validator:8.0.1.Final")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
