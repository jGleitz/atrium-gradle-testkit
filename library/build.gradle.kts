import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
	id("atrium-gradle-testkit-module")
}

atriumGradleTestkitModule {
	artifactId = "atrium-gradle-testkit"
}

dependencies {
	// TestKit belongs to the consumer's Gradle distribution, not this publication's runtime graph.
	compileOnly(gradleTestKit())
	api(libs.atrium.core)

	implementation(libs.atrium.logic)

	testImplementation(testLibs.atrium.fluent)
	testImplementation(gradleTestKit())
	testImplementation(testLibs.kotest.runner.junit5)
}

@OptIn(ExperimentalAbiValidation::class)
kotlin {
	explicitApi()
	abiValidation()
	sourceSets.test {
		kotlin.srcDir(rootProject.layout.projectDirectory.dir("compatibility-tests/src/test/kotlin"))
	}
}

// Both tasks use the reference files in `api/`: ordering lets `updateKotlinAbi checkKotlinAbi` run in one invocation.
tasks.named("checkKotlinAbi") {
	mustRunAfter("updateKotlinAbi")
}

dokka {
	dokkaSourceSets.configureEach {
		samples.from("src/test/kotlin/samples")
	}
}
