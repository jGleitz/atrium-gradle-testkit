import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
	kotlin("jvm")
}

dependencies {
	val spekVersion = "2.0.12"
	val atriumVersion = "0.13.0"

	api(project(":${rootProject.name}-logic"))
	api(project(":translations:${rootProject.name}-translation-en"))
	implementation(gradleTestKit())
	implementation(name = "atrium-core-api", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(name = "atrium-logic", version = atriumVersion, group = "ch.tutteli.atrium")

	testImplementation(project(":api-spec"))
	testImplementation(name = "spek-dsl-jvm", version = spekVersion, group = "org.spekframework.spek2")
	testRuntimeOnly(name = "spek-runtime-jvm", version = spekVersion, group = "org.spekframework.spek2")
	testRuntimeOnly(name = "spek-runner-junit5", version = spekVersion, group = "org.spekframework.spek2")

	constraints {
		testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:${KotlinCompilerVersion.VERSION}") {
			because("transitive dependencies refer to previous versions, but all Kotlin artefacts need to have the same version")
		}
	}
}

kotlin {
	explicitApi()
}

var willBePublished: Boolean by extra(true)
