plugins {
    `java-library`
}

dependencies {
    implementation("org.springframework:spring-web:5.3.30")
    implementation("org.springframework:spring-context:5.3.30")
    implementation("org.springframework:spring-webflux:5.3.30")
    implementation("org.thymeleaf:thymeleaf:3.0.15.RELEASE")
    implementation("commons-io:commons-io:2.7")
    implementation("io.netty:netty-transport:4.1.114.Final")
    implementation("io.netty:netty-handler:4.1.114.Final")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("io.projectreactor.netty:reactor-netty-http:1.1.23")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
}

tasks.test {
    useJUnitPlatform()
}
