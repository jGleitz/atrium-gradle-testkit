plugins {
	kotlin("jvm") /* version "1.4.10" */
}

repositories {
	jcenter()
}

dependencies {
	val kotestVersion: String by project
	val atriumVersion = "0.16.0"

	testImplementation(gradleTestKit())
	// for your project, use this instead:
	// testImplementation(name = "atrium-gradle-testkit-fluent-en", version = "<latest-release>", group = "de.joshuagleitze")
	testImplementation(project(":apis:atrium-gradle-testkit-fluent-en"))
	testImplementation(name = "atrium-fluent-en_GB", version = atriumVersion, group = "ch.tutteli.atrium")
	testImplementation(name = "kotest-runner-junit5", version = kotestVersion, group = "io.kotest")
}
