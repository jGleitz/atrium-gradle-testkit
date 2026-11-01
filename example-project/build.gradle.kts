plugins {
	id("atrium-gradle-testkit-kotlin")
}

dependencies {
	testImplementation(gradleTestKit())
	// for your project, use this instead:
	// testImplementation("de.joshuagleitze:atrium-gradle-testkit:<latest-release>")
	testImplementation(project(":library"))
	testImplementation(testLibs.atrium.fluent)
	testImplementation(testLibs.kotest.runner.junit5)
}
