import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath(buildLibs.plugin.kotlin.jvm)
	}
}

apply(plugin = "org.jetbrains.kotlin.jvm")

val testedLibraryVersion = providers.gradleProperty("testedLibraryVersion").get()
val compatibilityGradleVersion = providers.gradleProperty("compatibilityGradleVersion").get()
val stagedRepository = providers.gradleProperty("stagedRepository").get()
val distributionTestKit = configurations.detachedConfiguration(dependencies.gradleTestKit())

configure<KotlinJvmProjectExtension> {
	jvmToolchain(21)
	sourceSets.named("test") {
		// Checks of the runtime itself; the host does not run them, because it has no selected runtime.
		kotlin.srcDir("src/runtimeProvenance/kotlin")
	}
}

dependencies {
	add("testImplementation", "de.joshuagleitze:atrium-gradle-testkit:$testedLibraryVersion")
	// The TestKit client of the Gradle distribution that runs this build
	add("testImplementation", gradleTestKit())
	// Atrium publishes its implementation dependencies with runtime scope only, so the tests declare what they use.
	add("testImplementation", testLibs.atrium.api.fluent)
	add("testImplementation", testLibs.atrium.verbs)
	add("testImplementation", libs.atrium.logic)
	add("testImplementation", testLibs.kotest.runner.junit5)
}

tasks.named<Test>("test") {
	useJUnitPlatform()
	reports.junitXml.required = true

	val runtimeReport = layout.buildDirectory.file("compatibility-runtime/runtime.json")
	outputs.file(runtimeReport)
	systemProperty("compatibilityRuntimeReport", runtimeReport.get().asFile.absolutePath)
	systemProperty("compatibilityGradleVersion", compatibilityGradleVersion)
	systemProperty("compatibilityTestKitDir", layout.buildDirectory.dir("testkit").get().asFile.absolutePath)
	systemProperty("stagedRepository", stagedRepository)
	systemProperty("testedLibraryVersion", testedLibraryVersion)
	systemProperty("compatibilityDaemonGradleVersion", gradle.gradleVersion)
	systemProperty("compatibilityDaemonGradleHome", gradle.gradleHomeDir!!.absolutePath)
	systemProperty(
		"compatibilityTestKitJar",
		distributionTestKit.single { it.name == "gradle-test-kit-${gradle.gradleVersion}.jar" }.absolutePath
	)
	systemProperty("compatibilityDaemonJavaHome", providers.systemProperty("java.home").get())
	systemProperty("compatibilityDaemonJavaVersion", providers.systemProperty("java.version").get())
}
