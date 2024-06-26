plugins {
    id("backend.app-conventions")
}

group = "ru.dbhub"
version = "1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("rome:rome:1.0")
    runtimeOnly("org.postgresql:postgresql")

    implementation(project(":lib:collectsynchronizer"))
    implementation(project(":lib:commonconfig"))
    implementation(project(":lib:transport"))
}
