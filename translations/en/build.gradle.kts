plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit-translation-en"
}

dependencies {
	implementation(gradleTestKit())
	implementation(libs.atrium.core.api)
}
