plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(17)
}

val awsSdkVersion = "2.25.27"

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("software.amazon.awssdk:dynamodb:$awsSdkVersion")
    implementation("software.amazon.awssdk:ssm:$awsSdkVersion")
    implementation("software.amazon.awssdk:s3:$awsSdkVersion")
    implementation("software.amazon.awssdk:url-connection-client:$awsSdkVersion")
    implementation("org.json:json:20240303")

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

// Build a self-contained Fat JAR for AWS Lambda deployment
tasks.register<Jar>("lambdaFatJar") {
    archiveBaseName.set("unicompass-cloud-lambda")
    archiveClassifier.set("all")
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.example.orientation_app.cloud.OrientationLambdaHandler"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}
