plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit-logic"
}

dependencies {
	api(gradleTestKit())

	implementation(project(":translations:en"))
	implementation(libs.atrium.core.api)
	implementation(libs.atrium.logic)
}
