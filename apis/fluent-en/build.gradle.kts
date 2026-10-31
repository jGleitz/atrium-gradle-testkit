plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit-fluent-en"
}

dependencies {
	api(gradleTestKit())
	api(libs.atrium.core)

	implementation(project(":logic"))
	implementation(project(":translations:en"))
	implementation(libs.atrium.logic)

	testImplementation(project(":api-spec"))
	testImplementation(testLibs.kotest.runner.junit5)
}

kotlin {
	explicitApi()
}

dokka {
	dokkaSourceSets.configureEach {
		samples.from("src/test/kotlin/samples")
	}
}
