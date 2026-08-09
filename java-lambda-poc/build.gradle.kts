plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

repositories {
    mavenCentral()
}

dependencies {
    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")

    // AWS S3 SDK
    implementation("com.amazonaws:aws-java-sdk-s3:1.12.261")

    // AWS Step Functions SDK
    implementation("com.amazonaws:aws-java-sdk-stepfunctions:1.12.261")

    // CSV parsing
    implementation("com.opencsv:opencsv:5.9")

    // JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.12")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("java-lambda-poc")
    archiveClassifier.set("all")
    archiveVersion.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
