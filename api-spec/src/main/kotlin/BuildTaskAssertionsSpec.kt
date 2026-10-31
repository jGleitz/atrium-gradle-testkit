package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.notToThrow
import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import java.nio.file.Files
import java.nio.file.Path

/**
 * The behaviour every API must provide for assertions on a [BuildTask]. Concrete API modules provide their
 * assertion functions as lambdas.
 */
abstract class BuildTaskAssertionsSpec(
	toBeInvokedFeature: Expect<BuildTask?>.() -> Expect<BuildTask>,
	toBeInvokedWithCreator: Expect<BuildTask?>.(Expect<BuildTask>.() -> Unit) -> Expect<BuildTask?>,
	notToBeInvoked: Expect<BuildTask?>.() -> Expect<Unit>,
	toHaveOutcome: Expect<BuildTask?>.(TaskOutcome) -> Expect<BuildTask>,
	toBeSuccessful: Expect<BuildTask?>.() -> Expect<BuildTask>,
	toHaveFailed: Expect<BuildTask?>.() -> Expect<BuildTask>,
	toBeUpToDate: Expect<BuildTask?>.() -> Expect<BuildTask>,
	toBeSkipped: Expect<BuildTask?>.() -> Expect<BuildTask>,
	toHaveBeenLoadedFromCache: Expect<BuildTask?>.() -> Expect<BuildTask>,
	toHaveNoSource: Expect<BuildTask?>.() -> Expect<BuildTask>
): FunSpec({
	val fixtures = TemporaryFixtures(this, "atrium-gradle-testkit-build-task-")
	val outcomeRuns = mutableMapOf<TaskOutcome, BuildResult>()
	val successRun by lazy { outcomeRuns.getValue(SUCCESS) }

	beforeSpec {
		// every outcome gets its own project directory and Gradle runs, executed sequentially
		outcomeRuns[SUCCESS] = runSuccessFixture(fixtures.newDirectory("success"))
		outcomeRuns[FAILED] = runFailureFixture(fixtures.newDirectory("failed"))
		outcomeRuns[UP_TO_DATE] = runUpToDateFixture(fixtures.newDirectory("up-to-date"))
		outcomeRuns[SKIPPED] = runSkippedFixture(fixtures.newDirectory("skipped"))
		outcomeRuns[FROM_CACHE] = runFromCacheFixture(fixtures.newDirectory("from-cache"))
		outcomeRuns[NO_SOURCE] = runNoSourceFixture(fixtures.newDirectory("no-source"))
		fixtures.setupCompleted()
	}

	val toBeInvokedFragments = listOf("was:", "invoked")
	fun outcomeFragments(outcome: TaskOutcome) = toBeInvokedFragments + listOf("outcome:", outcome.name)

	registerSubjectLessTests(
		listOf(
			SubjectLessCase<BuildTask?>("toBeInvoked", toBeInvokedFragments) { toBeInvokedFeature() },
			SubjectLessCase<BuildTask?>("toBeInvoked { }", toBeInvokedFragments) {
				toBeInvokedWithCreator { toBeAnInstanceOf<BuildTask>() }
			},
			SubjectLessCase<BuildTask?>(
				"notToBeInvoked",
				listOf("was not:", "invoked")
			) { notToBeInvoked() },
			SubjectLessCase<BuildTask?>("toBeSuccessful", outcomeFragments(SUCCESS)) { toBeSuccessful() },
			SubjectLessCase<BuildTask?>("toHaveFailed", outcomeFragments(FAILED)) { toHaveFailed() },
			SubjectLessCase<BuildTask?>("toBeUpToDate", outcomeFragments(UP_TO_DATE)) { toBeUpToDate() },
			SubjectLessCase<BuildTask?>("toBeSkipped", outcomeFragments(SKIPPED)) { toBeSkipped() },
			SubjectLessCase<BuildTask?>("toHaveBeenLoadedFromCache", outcomeFragments(FROM_CACHE)) { toHaveBeenLoadedFromCache() },
			SubjectLessCase<BuildTask?>("toHaveNoSource", outcomeFragments(NO_SOURCE)) { toHaveNoSource() },
			SubjectLessCase<BuildTask?>("toHaveOutcome", outcomeFragments(UP_TO_DATE)) { toHaveOutcome(UP_TO_DATE) }
		)
	)

	/** Both overloads of the `toBeInvoked` assertion, reduced to “assert that the task was invoked”. */
	data class InvokedOverload(val name: String, val assertToBeInvoked: Expect<BuildTask?>.() -> Unit)

	val invokedOverloads = listOf(
		InvokedOverload("toBeInvoked") { toBeInvokedFeature().toBeAnInstanceOf<BuildTask>() },
		InvokedOverload("toBeInvoked { }") { toBeInvokedWithCreator { toBeAnInstanceOf<BuildTask>() } }
	)

	invokedOverloads.forEach { (name, assertToBeInvoked) ->
		context(name) {
			test("succeeds if the task was invoked directly") {
				expect {
					expect(successRun.task(":clean")).assertToBeInvoked()
				}.notToThrow()
			}

			test("succeeds if the task was invoked transitively") {
				expect {
					expect(successRun.task(":successTask")).assertToBeInvoked()
				}.notToThrow()
			}

			test("fails if the task was not invoked") {
				expect {
					expect(successRun.task(":iDontExist")).assertToBeInvoked()
				}.toThrow<AssertionError>().messageToContain("was", "invoked")
			}
		}
	}

	context("invocation creator validation and grouping") {
		test("fails if the assertionCreator does not create an assertion") {
			expect {
				expect(successRun.task(":clean")).toBeInvokedWithCreator { }
			}.toThrow<AssertionError>().messageToContain("at least one expectation defined")
		}

		test("reports nested outcome and path expectations together") {
			expect {
				expect(successRun.task(":successTask")).toBeInvokedWithCreator {
					feature(BuildTask::getOutcome).toEqual(FAILED)
					feature(BuildTask::getPath).toEqual(":notTheTask")
				}
			}.toThrow<AssertionError>().messageToContain(
				"getOutcome", SUCCESS.name, FAILED.name, "getPath", ":successTask", ":notTheTask"
			)
		}
	}

	context("notToBeInvoked") {
		test("fails if the task was invoked directly") {
			expect {
				expect(successRun.task(":clean")).notToBeInvoked()
			}.toThrow<AssertionError>().messageToContain("was not", "invoked")
		}

		test("fails if the task was invoked transitively") {
			expect {
				expect(successRun.task(":successTask")).notToBeInvoked()
			}.toThrow<AssertionError>().messageToContain("was not", "invoked")
		}

		test("succeeds if the task was not invoked") {
			expect {
				expect(successRun.task(":iDontExist")).notToBeInvoked()
			}.notToThrow()
		}
	}

	data class OutcomeCheck(
		val name: String,
		val assertOutcome: Expect<BuildTask?>.() -> Unit,
		val outcome: TaskOutcome,
		val task: String
	)

	val outcomeChecks = listOf(
		OutcomeCheck("toBeSuccessful", { toBeSuccessful() }, outcome = SUCCESS, task = ":successTask"),
		OutcomeCheck("toHaveFailed", { toHaveFailed() }, outcome = FAILED, task = ":failureTask"),
		OutcomeCheck("toBeUpToDate", { toBeUpToDate() }, outcome = UP_TO_DATE, task = ":upToDateTask"),
		OutcomeCheck("toBeSkipped", { toBeSkipped() }, outcome = SKIPPED, task = ":skippedTask"),
		OutcomeCheck("toHaveBeenLoadedFromCache", { toHaveBeenLoadedFromCache() }, outcome = FROM_CACHE, task = ":cachedTask"),
		OutcomeCheck("toHaveNoSource", { toHaveNoSource() }, outcome = NO_SOURCE, task = ":noSourceTask")
	)
	// the generic outcome check is registered after the conveniences, so that their test order is unchanged
	val genericOutcomeChecks = outcomeChecks.map { check ->
		check.copy(name = "toHaveOutcome(${check.outcome})", assertOutcome = { toHaveOutcome(check.outcome) })
	}
	(outcomeChecks + genericOutcomeChecks).forEach { (name, assertOutcome, outcome, task) ->
		context(name) {
			test("succeeds if the outcome is $outcome") {
				expect {
					expect(outcomeRuns.getValue(outcome).task(task)).assertOutcome()
				}.notToThrow()
			}

			outcomeChecks.filter { it.outcome != outcome }.forEach { otherCheck ->
				test("fails if the outcome is ${otherCheck.outcome}") {
					expect {
						expect(outcomeRuns.getValue(otherCheck.outcome).task(otherCheck.task)).assertOutcome()
					}.toThrow<AssertionError>().messageToContain("outcome", outcome.name, otherCheck.outcome.name)
				}
			}

			test("fails if the task was not invoked") {
				expect {
					expect(successRun.task("iDontExist")).assertOutcome()
				}.toThrow<AssertionError>().messageToContain(outcome.name, "was", "invoked")
			}
		}
	}
})

private fun BuildResult.requireOutcome(taskPath: String, expected: TaskOutcome) = apply {
	val actual = task(taskPath)?.outcome
	check(actual == expected) { "Fixture precondition: expected $taskPath to have outcome $expected, but was $actual" }
}

/** `:clean` is invoked directly, `:successTask` only transitively through `:aggregateTask`. */
private fun runSuccessFixture(projectDir: Path): BuildResult {
	projectDir.writeGradleProject(
		"""
		val successTask = tasks.register("successTask") {
			doLast { /* some action */ }
		}
		tasks.register<Delete>("clean") {
			delete(layout.buildDirectory)
		}
		tasks.register("aggregateTask") {
			dependsOn(successTask)
		}
		""".trimIndent()
	)
	return runGradle(projectDir, "clean", "aggregateTask").requireOutcome(":successTask", SUCCESS)
}

/** The build must fail because of `:failureTask`'s action, not because of an incidental error. */
private fun runFailureFixture(projectDir: Path): BuildResult {
	projectDir.writeGradleProject(
		"""
		tasks.register("failureTask") {
			doLast { error("I want to fail") }
		}
		""".trimIndent()
	)
	val result = runGradle(projectDir, "failureTask", fail = true).requireOutcome(":failureTask", FAILED)
	val failedTasks = result.tasks.filter { it.outcome == FAILED }.map { it.path }
	check(failedTasks == listOf(":failureTask")) { "Fixture precondition: expected only :failureTask to fail, but failed: $failedTasks" }
	check("Execution failed for task ':failureTask'" in result.output && "> I want to fail" in result.output) {
		"Fixture precondition: the build in $projectDir did not fail because of the intended error of :failureTask"
	}
	println("FIXTURE-FAILURE task=:failureTask cause=I want to fail dir=$projectDir")
	return result
}

/** A first run produces the declared output from the declared input; the identical second run is up to date. */
private fun runUpToDateFixture(projectDir: Path): BuildResult {
	projectDir.resolve("input.txt").toFile().writeText("up-to-date input")
	projectDir.writeGradleProject(copyTask("upToDateTask", outputDir = "up-to-date", cacheable = false))
	runGradle(projectDir, "upToDateTask").requireOutcome(":upToDateTask", SUCCESS)
	return runGradle(projectDir, "upToDateTask").requireOutcome(":upToDateTask", UP_TO_DATE)
}

private fun runSkippedFixture(projectDir: Path): BuildResult {
	projectDir.writeGradleProject(
		"""
		tasks.register("skippedTask") {
			onlyIf { false }
			doLast { /* some action */ }
		}
		""".trimIndent()
	)
	return runGradle(projectDir, "skippedTask").requireOutcome(":skippedTask", SKIPPED)
}

/** Seeds a build cache that belongs to this fixture alone, deletes the task output, then restores it from the cache. */
private fun runFromCacheFixture(fixtureDir: Path): BuildResult {
	val projectDir = Files.createDirectory(fixtureDir.resolve("project"))
	val cacheDir = Files.createDirectory(fixtureDir.resolve("build-cache"))
	val outputFile = projectDir.resolve("build/cached/output.txt")
	val cachePath = cacheDir.toAbsolutePath().toString().replace('\\', '/')
	projectDir.resolve("input.txt").toFile().writeText("cached input")
	projectDir.writeGradleProject(
		copyTask("cachedTask", outputDir = "cached", cacheable = true) + "\n" +
			"""
			tasks.register<Delete>("clean") {
				delete(layout.buildDirectory)
			}
			""".trimIndent(),
		settingsScript = """
			buildCache {
				local {
					directory = File("$cachePath")
				}
			}
			""".trimIndent()
	)
	check(Files.list(cacheDir).use { it.count() } == 0L) { "Fixture precondition: the build cache $cacheDir is not empty" }

	val seed = runGradle(projectDir, "cachedTask", "--build-cache", "--info").requireOutcome(":cachedTask", SUCCESS)
	check("Stored cache entry for task ':cachedTask'" in seed.output) { "Fixture precondition: :cachedTask was not stored in $cacheDir" }
	println("FIXTURE-CACHE seed: " + seed.output.lines().filter { "cache entry" in it }.joinToString(" | "))

	runGradle(projectDir, "clean")
	check(!Files.exists(outputFile)) { "Fixture precondition: clean did not delete $outputFile" }
	println("FIXTURE-CACHE clean: deleted $outputFile")

	val restore = runGradle(projectDir, "cachedTask", "--build-cache", "--info").requireOutcome(":cachedTask", FROM_CACHE)
	check("Loaded cache entry for task ':cachedTask'" in restore.output) { "Fixture precondition: :cachedTask was not loaded from $cacheDir" }
	check(outputFile.toFile().readText() == "cached input") { "Fixture precondition: $outputFile was not restored" }
	println("FIXTURE-CACHE restore: " + restore.output.lines().filter { "cache entry" in it }.joinToString(" | "))
	return restore
}

private fun runNoSourceFixture(projectDir: Path): BuildResult {
	projectDir.writeGradleProject(
		"""
		tasks.register("noSourceTask") {
			inputs.files(layout.projectDirectory.dir("no-source")).skipWhenEmpty().ignoreEmptyDirectories()
			doLast { /* some action */ }
		}
		""".trimIndent()
	)
	return runGradle(projectDir, "noSourceTask").requireOutcome(":noSourceTask", NO_SOURCE)
}

/** A task that copies the declared input file `input.txt` to a declared output file in the build directory. */
private fun copyTask(name: String, outputDir: String, cacheable: Boolean) =
	"""
	tasks.register("$name") {
		val input = layout.projectDirectory.file("input.txt")
		val output = layout.buildDirectory.file("$outputDir/output.txt")
		inputs.file(input)
		outputs.file(output)
		outputs.cacheIf { $cacheable }
		doLast { output.get().asFile.writeText(input.asFile.readText()) }
	}
	""".trimIndent()
