import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	`kotlin-dsl`
}

// The build logic must run on any JVM that can run Gradle, not only on the one that mise selects
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
