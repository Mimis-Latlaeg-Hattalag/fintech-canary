import org.slf4j.LoggerFactory

val useJavaVersion: String by project

private val log by lazy { LoggerFactory.getLogger("me.riddle.fintech.canary.build") }

plugins {
    `kotlin-dsl`
}

allprojects {
    repositories {
        mavenCentral()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(useJavaVersion))
        vendor.set(JvmVendorSpec.ADOPTIUM)
        log.info("\t|=> Riddle me that Java Toolchain SET to    -> $useJavaVersion : ${JvmVendorSpec.ADOPTIUM}.")
    }
}

dependencies {
    implementation(gradleApi())
    implementation(platform(kotlin("bom")))

    api(libs.slf4j.api)
    implementation(libs.logback.classic)

}

