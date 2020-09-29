import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
	kotlin("jvm")
}

dependencies {
	val spekVersion = "2.0.12"
	val atriumVersion = "0.13.0"

	implementation(project(":translations:${rootProject.name}-translation-en"))
	implementation(gradleTestKit())
	implementation(name = "atrium-core-api", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(name = "atrium-logic", version = atriumVersion, group = "ch.tutteli.atrium")

	testImplementation(name = "spek-dsl-jvm", version = spekVersion, group = "org.spekframework.spek2")
	testImplementation(name = "spek-runtime-jvm", group = "org.spekframework.spek2", version = spekVersion)
	testImplementation(name = "atrium-fluent-en_GB", version = "0.13.0", group = "ch.tutteli.atrium")
	testRuntimeOnly(name = "spek-runner-junit5", version = spekVersion, group = "org.spekframework.spek2")

	constraints {
		testImplementation("org.jetbrains.kotlin:kotlin-reflect:${KotlinCompilerVersion.VERSION}") {
			because("transitive dependencies refer to previous versions, but all Kotlin artefacts need to have the same version")
		}
	}
}

var willBePublished: Boolean by extra(true)
