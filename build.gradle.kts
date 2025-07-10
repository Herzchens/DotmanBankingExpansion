plugins {
    id("java")
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://m2.dv8tion.net/releases")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    implementation("net.dv8tion:JDA:5.0.0-beta.20")

    implementation("club.minnced:jda-ktx:0.11.0-beta.20")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

tasks {
    shadowJar {
        archiveFileName.set("DotmanBankingExpansion.jar")
        relocate("net.dv8tion.jda", "com.herzchen.dotmanbankingexpansion.lib.jda")
        relocate("club.minnced", "com.herzchen.dotmanbankingexpansion.lib.jdakt")

        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}