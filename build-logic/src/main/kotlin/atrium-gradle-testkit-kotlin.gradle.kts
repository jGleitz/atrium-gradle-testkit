import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
	id("org.jetbrains.kotlin.jvm")
}

repositories {
	mavenCentral()
}

val javaReleaseVersion = providers.gradleProperty("javaReleaseVersion").map(JavaLanguageVersion::of)
val javaToolchainVersion = providers.gradleProperty("javaVersion")
	.map(JavaLanguageVersion::of)
	.orElse(javaReleaseVersion)
val kotlinReleaseVersion = providers.gradleProperty("kotlinReleaseVersion").map(KotlinVersion::fromVersion)

java {
	toolchain {
		languageVersion = javaToolchainVersion
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = javaReleaseVersion.map(JavaLanguageVersion::asInt)
}

kotlin {
	compilerOptions {
		languageVersion = kotlinReleaseVersion
		apiVersion = kotlinReleaseVersion
		jvmTarget = javaReleaseVersion.map { JvmTarget.fromTarget(it.toString()) }
	}
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	reports.junitXml.required = true
	systemProperty("kotlinVersion", kotlinReleaseVersion.get().version)
	systemProperty("javaVersion", javaReleaseVersion.get().toString())
}
