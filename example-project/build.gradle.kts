plugins {
	kotlin("jvm") /* version "1.4.10" */
}

repositories {
	mavenCentral()
}

dependencies {
	val kotestVersion: String by project
	val atriumVersion = "0.16.0"

	testImplementation(gradleTestKit())
	// for your project, use this instead:
	// testImplementation("de.joshuagleitze:atrium-gradle-testkit-fluent-en:<latest-release>")
	testImplementation(project(":apis:atrium-gradle-testkit-fluent-en"))
	testImplementation("ch.tutteli.atrium:atrium-fluent-en_GB:$atriumVersion")
	testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}
