import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

plugins {
    id("backend.java-conventions")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
