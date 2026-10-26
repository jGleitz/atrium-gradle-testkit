plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit-fluent-en"
}

dependencies {
	api(project(":logic"))
	api(project(":translations:en"))
	api(gradleTestKit())

	implementation(libs.atrium.core.api)
	implementation(libs.atrium.logic)

	testImplementation(project(":api-spec"))
	testImplementation(testLibs.kotest.runner.junit5)
}

kotlin {
	explicitApi()
}
