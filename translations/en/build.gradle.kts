plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit-translation-en"
}

dependencies {
	api(libs.atrium.core.api)
}
