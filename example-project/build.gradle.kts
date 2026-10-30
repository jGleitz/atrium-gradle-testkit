plugins {
	id("atrium-gradle-testkit-kotlin")
}

dependencies {
	testImplementation(gradleTestKit())
	// for your project, use this instead:
	// testImplementation("de.joshuagleitze:atrium-gradle-testkit-fluent-en:<latest-release>")
	testImplementation(project(":apis:fluent-en"))
	testImplementation(testLibs.atrium.fluent)
	testImplementation(testLibs.kotest.runner.junit5)
}
