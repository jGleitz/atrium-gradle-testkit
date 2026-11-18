import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.tools.ExecFileLoader
import java.util.zip.ZipInputStream

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath(buildLibs.plugin.kotlin.jvm)
		classpath(buildLibs.jacoco.core)
	}
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "jacoco")

val testedLibraryVersion = providers.gradleProperty("testedLibraryVersion").get()
val compatibilityGradleVersion = providers.gradleProperty("compatibilityGradleVersion").get()
val stagedRepository = providers.gradleProperty("stagedRepository").get()
val distributionTestKit = configurations.detachedConfiguration(dependencies.gradleTestKit())
val stagedArtifact = file("$stagedRepository/de/joshuagleitze/atrium-gradle-testkit/$testedLibraryVersion")
val productionJar = stagedArtifact.resolve("atrium-gradle-testkit-$testedLibraryVersion.jar")
val productionClasses = zipTree(productionJar).matching { include("**/*.class") }
val productionSources = zipTree(stagedArtifact.resolve("atrium-gradle-testkit-$testedLibraryVersion-sources.jar"))
val coverageData = layout.buildDirectory.file("jacoco/test.exec")
val coverageXml = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")

configure<JacocoPluginExtension> {
	toolVersion = buildLibs.jacoco.core.get().versionConstraint.requiredVersion
}

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
	extensions.configure<JacocoTaskExtension> {
		destinationFile = coverageData.get().asFile
		// The shared tests have the same package as the library. Select exact JAR entries, not a package wildcard.
		// Track the JAR contents as a configuration-cache input: restaging can add new classes.
		val jarBytes = providers.fileContents(layout.projectDirectory.file(productionJar.absolutePath)).asBytes.orNull
			?: throw GradleException("The staged production JAR $productionJar does not exist. Stage the library first.")
		includes = ZipInputStream(jarBytes.inputStream()).use { jar ->
			generateSequence { jar.nextEntry }.map { it.name }.filter { it.endsWith(".class") }
				.map { it.removeSuffix(".class").replace('/', '.') }.toList()
		}
	}

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

// Gradle's JaCoCo tasks otherwise skip silently when execution data is absent.
val coverageInputs = tasks.register("checkCoverageInputs") {
	dependsOn(tasks.named("test"))
	val data = coverageData.get().asFile
	val classes = productionClasses
	val sources = productionSources
	val jar = productionJar
	inputs.file(coverageData)
	inputs.files(productionClasses)
	inputs.files(productionSources)
	doLast {
		check(data.length() > 0) { "Missing or empty production coverage execution data" }
		check(!classes.isEmpty) { "The staged production JAR contains no classes" }
		check(!sources.isEmpty) { "The staged sources JAR is empty" }
		val execution = ExecFileLoader().apply { load(data) }
		check(execution.executionDataStore.contents.isNotEmpty()) { "No production coverage execution records" }
		val coverage = CoverageBuilder()
		Analyzer(execution.executionDataStore, coverage).analyzeAll(jar)
		val classIds = coverage.classes.associate { it.name to it.id }
		val mismatches = execution.executionDataStore.contents.filter { classIds[it.name] != it.id }
		check(mismatches.isEmpty()) {
			"Production coverage class-ID mismatch: ${mismatches.joinToString { it.name }}"
		}
	}
}

val coverageSources = tasks.register<Sync>("extractCoverageSources") {
	// Kotlin source paths need not mirror their package. JaCoCo looks up source files by package name.
	from(productionSources) {
		include("**/*.kt")
		eachFile {
			val packageName = file.useLines { lines -> lines.first { it.startsWith("package ") }.removePrefix("package ") }
			relativePath = RelativePath(true, *packageName.split('.').toTypedArray(), name)
		}
	}
	into(layout.buildDirectory.dir("coverage/sources"))
}

val coverageReport = tasks.named<JacocoReport>("jacocoTestReport") {
	dependsOn(coverageInputs, coverageSources)
	executionData.setFrom(coverageData)
	classDirectories.setFrom(productionClasses)
	sourceDirectories.setFrom(coverageSources.map { it.destinationDir })
	reports {
		xml.required = true
		xml.outputLocation = coverageXml
		html.required = true
	}
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
	dependsOn(coverageReport)
	inputs.file(coverageXml)
	executionData.setFrom(coverageData)
	classDirectories.setFrom(productionClasses)
	violationRules {
		rule {
			listOf("INSTRUCTION", "BRANCH").forEach { counterName ->
				limit {
					counter = counterName
					value = "MISSEDCOUNT"
					maximum = BigDecimal.ZERO
				}
			}
			limit {
				counter = "INSTRUCTION"
				value = "COVEREDCOUNT"
				minimum = BigDecimal.ONE
			}
		}
	}
}

tasks.named("check") {
	dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
