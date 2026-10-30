plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit-logic"
}

dependencies {
	api(gradleTestKit())
	api(libs.atrium.core)
	api(libs.atrium.logic)

	implementation(project(":translations:en"))
}
