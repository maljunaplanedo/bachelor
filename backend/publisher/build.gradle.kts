plugins {
    id("backend.app-conventions")
}

group = "ru.dbhub"
version = "1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    runtimeOnly("org.postgresql:postgresql")

    implementation(project(":lib:commonconfig"))
    implementation(project(":lib:transport"))
    implementation(project(":lib:collectsynchronizer"))
}
