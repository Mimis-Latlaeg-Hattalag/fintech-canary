plugins {
    kotlin("jvm")
    java
}

dependencies {
    implementation(libs.fasterxml.jackson)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
