plugins {
    kotlin("jvm")
    java
}

dependencies {
    implementation(project(":domain"))


    implementation(libs.vavr)               // Cuz Java can't
    implementation(libs.fasterxml.jackson)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.logback.classic)
}

tasks.test {
    useJUnitPlatform()
}
