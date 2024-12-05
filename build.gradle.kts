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
    implementation("com.google.code.gson:gson:2.9.0")
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("com.guestbook.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.guestbook.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
} 