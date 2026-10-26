plugins {
	id("atrium-gradle-testkit-kotlin")
}

dependencies {
	api(libs.atrium.specs) {
		exclude(group = "org.spekframework.spek2")
		exclude(group = "ch.tutteli.spek")
	}
	implementation(gradleTestKit())
	implementation(project(":translations:en"))
	implementation(libs.atrium.fluent.en.gb)
	implementation(testLibs.kotest.runner.junit5)
}
