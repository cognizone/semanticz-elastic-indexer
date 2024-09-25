plugins {
    id("java")
    id("org.owasp.dependencycheck") version "10.0.3"
    id("maven-publish")
    id("signing")
    id("pl.allegro.tech.build.axion-release") version "1.13.3"
}

group = "zone.cogni.semanticz"

// Configure the axion-release plugin
scmVersion {
    tag.apply {
        prefix = "v"
        versionSeparator = ""
        branchPrefix = mapOf(
                "release/.*" to "release-v",
                "hotfix/.*" to "hotfix-v"
        )
    }

    nextVersion.apply {
        suffix = "SNAPSHOT"
        separator = "-"
    }

    versionIncrementer("incrementPatch") // Increment the patch version
}

// Set the project version from scmVersion
version = scmVersion.version

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("jakarta.json:jakarta.json-api:2.0.1")
    implementation("co.elastic.clients:elasticsearch-java:7.17.24")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.3")
    implementation("org.slf4j:jcl-over-slf4j:1.7.36")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("semanticz-elastic-indexer")
                description.set("The Elastic Indexer is a Java-based tool that helps in indexing and searching documents in an Elasticsearch cluster using simple and bulk methods.")
                url.set("https://github.com/cognizone/elastic-indexer")

                scm {
                    connection.set("scm:git@github.com:cognizone/elastic-indexer.git")
                    developerConnection.set("scm:git@github.com:cognizone/semanticz-elastic-indexer.git")
                    url.set("https://github.com/cognizone/semanticz-elastic-indexer.git")
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
                        email.set("dev@cognizone.com")
                    }
                }
            }
        }
    }
}

tasks.register("qualityCheck") {
    dependsOn(tasks.named("pmdMain"))
    dependsOn(tasks.named("pmdTest"))
    dependsOn(tasks.named("dependencyCheckAnalyze"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(projectDir) {
        include("LICENSE")
        into("META-INF")
    }
}
