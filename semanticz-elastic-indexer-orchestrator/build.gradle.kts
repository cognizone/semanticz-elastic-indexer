plugins {
    `java-library`
}

dependencies {
    implementation(project(":semanticz-elastic-indexer"))
    implementation(project(":semanticz-webflux-tools"))
    implementation("co.elastic.clients:elasticsearch-java:7.17.24")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("org.apache.jena:jena-core:4.8.0")
    implementation("org.apache.jena:jena-arq:4.8.0")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("zone.cogni.asquare:access:0.7.0")
    implementation("zone.cogni.semanticz:semanticz-rdf2jsonld:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
}

tasks.test {
    useJUnitPlatform()
}
