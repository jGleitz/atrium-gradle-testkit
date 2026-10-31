plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit"
}

dependencies {
	api(gradleTestKit())
	api(libs.atrium.core)

	implementation(libs.atrium.logic)

	testImplementation(testLibs.atrium.fluent)
	testImplementation(testLibs.kotest.runner.junit5)
}

kotlin {
	explicitApi()
	sourceSets.test {
		kotlin.srcDir(rootProject.layout.projectDirectory.dir("compatibility-tests/src/test/kotlin"))
	}
}

dokka {
	dokkaSourceSets.configureEach {
		samples.from("src/test/kotlin/samples")
	}
}
