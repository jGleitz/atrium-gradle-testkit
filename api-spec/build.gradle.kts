plugins {
	kotlin("jvm")
}

dependencies {
	val spekVersion: String by project
	val atriumVersion: String by project

	api(name = "atrium-specs", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(gradleTestKit())
	implementation(project(":translations:${rootProject.name}-translation-en"))
	implementation(name = "atrium-fluent-en_GB", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(name = "spek-dsl-jvm", version = spekVersion, group = "org.spekframework.spek2")
	implementation(name = "spek-testfiles", version = "1.0.2", group = "de.joshuagleitze")
}
