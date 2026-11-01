import java.security.MessageDigest
import javax.inject.Inject
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
	id("org.jetbrains.kotlin.jvm")
}

fun requiredProperty(name: String, purpose: String): String =
	providers.gradleProperty(name).orNull ?: throw GradleException("Missing required property -P$name ($purpose)")

val consumerBuildDir = File(requiredProperty("consumerBuildDir", "absolute build directory of this compiler row"))
check(consumerBuildDir.isAbsolute) { "-PconsumerBuildDir must be an absolute path, but was '$consumerBuildDir'" }
layout.buildDirectory = consumerBuildDir

val testedLibraryVersion = requiredProperty("testedLibraryVersion", "version of the staged library to consume")

kotlin {
	jvmToolchain(21)
}

dependencies {
	implementation("de.joshuagleitze:atrium-gradle-testkit:$testedLibraryVersion")
	implementation("ch.tutteli.atrium:atrium-api-fluent:1.3.0")
	implementation("ch.tutteli.atrium:atrium-verbs:1.3.0")
	// TestKit of the Gradle distribution running this build, just like in a consumer's plugin tests.
	implementation(gradleTestKit())
}

/** Runs the probe and records its standard output, which [VerifyConsumer] checks. */
abstract class RunConsumerProbe @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {
	@get:Classpath
	abstract val classpath: ConfigurableFileCollection

	@get:Input
	abstract val mainClass: Property<String>

	@get:Nested
	abstract val launcher: Property<JavaLauncher>

	@get:OutputFile
	abstract val probeOutput: RegularFileProperty

	@TaskAction
	fun run() {
		probeOutput.get().asFile.outputStream().use { output ->
			execOperations.javaexec {
				executable = launcher.get().executablePath.asFile.absolutePath
				classpath = this@RunConsumerProbe.classpath
				mainClass = this@RunConsumerProbe.mainClass
				standardOutput = output
			}
		}
	}
}

/**
 * Fails unless the probe ran to completion (`CONSUMER_OK`) and loaded the library's classes from the resolved staged
 * artifact, so that a build that merely prints the sentinel is not mistaken for library consumption.
 */
abstract class VerifyConsumer : DefaultTask() {
	@get:InputFile
	abstract val probeOutput: RegularFileProperty

	@get:InputFiles
	abstract val libraryArtifact: ConfigurableFileCollection

	@TaskAction
	fun verify() {
		val lines = probeOutput.get().asFile.readLines()
		lines.forEach { logger.lifecycle(it) }
		val library = libraryArtifact.files.singleOrNull()
			?: throw GradleException("Expected exactly one resolved library artifact, but got ${libraryArtifact.files}")
		val loadedFrom = lines.singleOrNull { it.startsWith(LIBRARY_PREFIX) }?.removePrefix(LIBRARY_PREFIX)
			?: throw GradleException("The probe did not report which library artifact it loaded ($LIBRARY_PREFIX)")
		if (File(loadedFrom).canonicalFile != library.canonicalFile) {
			throw GradleException("The probe loaded the library from $loadedFrom instead of the staged artifact $library")
		}
		if (lines.lastOrNull() != "CONSUMER_OK") {
			throw GradleException("The probe did not print the success sentinel CONSUMER_OK as its last line")
		}
		val sha256 = MessageDigest.getInstance("SHA-256").digest(library.readBytes()).joinToString("") { "%02x".format(it) }
		logger.lifecycle("CONSUMER_LIBRARY_SHA256=$sha256")
	}

	private companion object {
		const val LIBRARY_PREFIX = "CONSUMER_LIBRARY_JAR="
	}
}

val runConsumerProbe by tasks.registering(RunConsumerProbe::class) {
	description = "Runs the compiled consumer probe against the staged library."
	classpath.from(sourceSets.main.map { it.runtimeClasspath })
	mainClass = "de.joshuagleitze.test.gradle.consumer.ConsumerProbeKt"
	launcher = javaToolchains.launcherFor(java.toolchain)
	probeOutput = layout.buildDirectory.file("consumer-probe/output.txt")
}

tasks.register<VerifyConsumer>("verifyConsumer") {
	description = "Compiles and runs the consumer probe and verifies that it consumed the staged library successfully."
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	dependsOn(tasks.compileKotlin)
	probeOutput = runConsumerProbe.flatMap { it.probeOutput }
	libraryArtifact.from(
		configurations.runtimeClasspath.map { classpath ->
			classpath.incoming.artifactView {
				componentFilter { it is ModuleComponentIdentifier && it.group == "de.joshuagleitze" && it.module == "atrium-gradle-testkit" }
			}.files
		}
	)
}
