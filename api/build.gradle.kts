plugins {
    kotlin("jvm")
    java
    application
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(libs.vavr)

    api(libs.slf4j.api)
    implementation(libs.logback.classic)


    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("me.riddle.fintech.api.Main")
}

tasks.test {
    useJUnitPlatform()
}