plugins {
	id("atrium-gradle-testkit-kotlin")
}

dependencies {
	api(testLibs.atrium.specs) {
		exclude(group = "org.spekframework.spek2")
		exclude(group = "ch.tutteli.spek")
	}
	implementation(gradleTestKit())
	implementation(project(":translations:en"))
	implementation(testLibs.atrium.fluent.en.gb)
	implementation(testLibs.kotest.runner.junit5)
}
