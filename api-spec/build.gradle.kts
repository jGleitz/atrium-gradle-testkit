plugins {
	kotlin("jvm")
}

dependencies {
	val spekVersion = "2.0.12"
	val atriumVersion = "0.13.0"

	api(name = "atrium-specs", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(gradleTestKit())
	implementation(project(":translations:${rootProject.name}-translation-en"))
	implementation(name = "atrium-fluent-en_GB", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(name = "spek-dsl-jvm", version = spekVersion, group = "org.spekframework.spek2")
	implementation(name = "spek-testfiles", version = "1.0.1", group = "de.joshuagleitze")
}
