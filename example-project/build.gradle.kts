plugins {
	kotlin("jvm") /* version "1.4.10" */
}

repositories {
	jcenter()
}

dependencies {
	val spekVersion = "2.0.15"
	val atriumVersion = "0.15.0"

	testImplementation(gradleTestKit())
	// for your project, use this instead:
	// testImplementation(name = "atrium-gradle-testkit-fluent-en", version = "<latest-release>", group = "de.joshuagleitze")
	testImplementation(project(":apis:atrium-gradle-testkit-fluent-en"))
	testImplementation(name = "spek-testfiles", version = "1.0.2", group = "de.joshuagleitze")
	testImplementation(name = "atrium-fluent-en_GB", version = atriumVersion, group = "ch.tutteli.atrium")
	testImplementation(name = "spek-dsl-jvm", version = spekVersion, group = "org.spekframework.spek2")
	testRuntimeOnly(name = "spek-runtime-jvm", version = spekVersion, group = "org.spekframework.spek2")
	testRuntimeOnly(name = "spek-runner-junit5", version = spekVersion, group = "org.spekframework.spek2")
}
