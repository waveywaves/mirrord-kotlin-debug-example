plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "com.guestbook"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("redis.clients:jedis:4.3.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("com.guestbook.MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.guestbook.MainKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}