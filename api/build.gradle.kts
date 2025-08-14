plugins {
    kotlin("jvm")
    java
    application
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(libs.vavr)
    implementation(libs.fasterxml.jackson)

    api(libs.slf4j.api)
    implementation(libs.logback.classic)


    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("me.riddle.fintech.api.EntryPoint")
}

tasks.test {
    useJUnitPlatform()
}

// Configure the run task to handle stdin
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

// Custom task for interactive mode
tasks.register<JavaExec>("interactive") {
    group = "application"
    description = "Run the PagerDuty User Explorer in interactive mode"
    mainClass.set("me.riddle.fintech.api.EntryPoint")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--interactive")
    standardInput = System.`in`

    doFirst {
        if (System.getenv("PAGERDUTY_API_TOKEN") == null) {
            throw GradleException("PAGERDUTY_API_TOKEN environment variable not set")
        }
    }
}