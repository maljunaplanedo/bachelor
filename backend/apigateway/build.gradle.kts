plugins {
    id("backend.app-conventions")
}

group = "ru.dbhub"
version = "1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation(project(":lib:commonconfig"))
    implementation(project(":lib:transport"))
}
