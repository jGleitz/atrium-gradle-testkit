plugins {
	id("atrium-gradle-testkit-kotlin")
}

dependencies {
	api(gradleTestKit())
	api(testLibs.kotest.runner.junit5)
	api(testLibs.atrium.fluent.en.gb)

	implementation(project(":translations:en"))
}
