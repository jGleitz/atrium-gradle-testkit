plugins {
	kotlin("jvm")
}

dependencies {
	val spekVersion: String by project
	val atriumVersion: String by project

	api(project(":${rootProject.name}-logic"))
	api(project(":translations:${rootProject.name}-translation-en"))
	implementation(gradleTestKit())
	implementation(name = "atrium-core-api", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(name = "atrium-logic", version = atriumVersion, group = "ch.tutteli.atrium")

	testImplementation(project(":api-spec"))
	testImplementation(name = "spek-dsl-jvm", version = spekVersion, group = "org.spekframework.spek2")
	testRuntimeOnly(name = "spek-runtime-jvm", version = spekVersion, group = "org.spekframework.spek2")
	testRuntimeOnly(name = "spek-runner-junit5", version = spekVersion, group = "org.spekframework.spek2")
}

kotlin {
	explicitApi()
}

var willBePublished: Boolean by extra(true)
