// Root build.gradle.kts

import org.gradle.api.tasks.testing.Test
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.plugins.signing.SigningExtension
import org.gradle.jvm.tasks.Jar

plugins {
    id("pl.allegro.tech.build.axion-release") version "1.13.3"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Add the dependency-check plugin to the classpath
        classpath("org.owasp:dependency-check-gradle:8.4.0") // Use the latest version
    }
}

group = "zone.cogni.semanticz"

// Configure the axion-release plugin
scmVersion {
    tag.apply {
        prefix = "v"
        versionSeparator = ""
        branchPrefix["release/.*"] = "release-v"
        branchPrefix["hotfix/.*"] = "hotfix-v"
    }
    nextVersion.apply {
        suffix = "SNAPSHOT"
        separator = "-"
    }
    versionIncrementer("incrementPatch") // Increment the patch version
}

// Set the version based on the presence of a command-line parameter otherwise set the project version from scmVersion
version = findProperty("publishVersion") as String? ?: scmVersion.version

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    // Apply plugins
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.owasp.dependencycheck")

    group = "zone.cogni.semanticz"
    version = rootProject.version

    tasks.withType<JavaCompile> {
        options.release.set(11)
    }

    // Configure the Java plugin
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(11))
        withJavadocJar()
        withSourcesJar()
    }

    // Include LICENSE file in META-INF folder of the jar
    tasks.withType<Jar> {
        from(rootProject.projectDir) {
            include("LICENSE")
            into("META-INF")
        }
    }

    // Configure the test task
    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Publishing configuration
    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("Description for ${project.name}")
                    url.set("https://github.com/cognizone/elastic-indexer")

                    scm {
                        connection.set("scm:git@github.com/cognizone/elastic-indexer.git")
                        developerConnection.set("scm:git@github.com/cognizone/elastic-indexer.git")
                        url.set("https://github.com/cognizone/elastic-indexer.git")
                    }

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("cognizone")
                            name.set("Cognizone")
                            email.set("semanticz@cogni.zone")
                        }
                    }
                }
            }
        }

        repositories {
            // Cognizone Nexus repository
            if (project.hasProperty("publishToCognizoneNexus")) {
                maven {
                    credentials {
                        username = System.getProperty("nexus.username")
                        password = System.getProperty("nexus.password")
                    }
                    val releasesRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-release"
                    val snapshotsRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-snapshot"
                    url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                    isAllowInsecureProtocol = true
                }
            }

            // Maven Central repository
            if (project.hasProperty("publishToMavenCentral")) {
                maven {
                    credentials {
                        username = System.getProperty("ossrh.username")
                        password = System.getProperty("ossrh.password")
                    }
                    val stagingRepoUrl = "${System.getProperty("ossrh.url")}/service/local/staging/deploy/maven2"
                    val snapshotsRepoUrl = "${System.getProperty("ossrh.url")}/content/repositories/snapshots"
                    url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else stagingRepoUrl)
                }
            }
        }
    }

    // Signing configuration
    extensions.configure<SigningExtension> {
        if (project.hasProperty("publishToMavenCentral") || project.hasProperty("publishToCognizoneNexus")) {
            useInMemoryPgpKeys(
                    System.getenv("SIGNING_KEY"),
                    System.getenv("SIGNING_PASSWORD")
            )
            sign(extensions.getByType<PublishingExtension>().publications["mavenJava"])
        }
    }

    // Quality check tasks
    tasks.register("qualityCheck") {
        dependsOn("pmdMain", "pmdTest", "dependencyCheckAnalyze")
    }
}
