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
}

tasks.test {
    useJUnitPlatform()
}