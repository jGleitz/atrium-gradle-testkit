import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	`kotlin-dsl`
}

// The build logic runs in the Gradle daemon, so it targets the oldest Java version that Gradle supports
tasks.compileJava {
	options.release = 17
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}

dependencies {
	implementation(buildLibs.plugin.dokka)
	implementation(buildLibs.plugin.kotlin.jvm)
	implementation(buildLibs.plugin.vanniktech.maven.publish)
}
