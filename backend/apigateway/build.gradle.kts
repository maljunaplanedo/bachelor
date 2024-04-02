plugins {
    id("backend.app-conventions")
}

group = "ru.dbhub"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation(project(":lib:transport"))
    implementation(project(":lib:mvcerror"))
}
