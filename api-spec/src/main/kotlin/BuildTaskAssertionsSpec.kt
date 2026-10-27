package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.isA
import ch.tutteli.atrium.api.fluent.en_GB.messageContains
import ch.tutteli.atrium.api.fluent.en_GB.notToThrow
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.specs.Feature0
import ch.tutteli.atrium.specs.Fun1
import ch.tutteli.atrium.specs.forSubjectLess
import ch.tutteli.atrium.specs.lambda
import ch.tutteli.atrium.specs.name
import ch.tutteli.atrium.specs.unifySignatures
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.INVOKED
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.WAS
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.WAS_NOT
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

abstract class BuildTaskAssertionsSpec(
	wasInvokedFeature: Feature0<BuildTask?, BuildTask>,
	wasInvokedFun: Fun1<BuildTask?, Expect<BuildTask>.() -> Unit>,
	wasNotInvoked: Feature0<BuildTask?, Unit>,
	wasSuccessful: Feature0<BuildTask?, BuildTask>,
	failed: Feature0<BuildTask?, BuildTask>,
	wasUpToDate: Feature0<BuildTask?, BuildTask>,
	wasSkipped: Feature0<BuildTask?, BuildTask>,
	usedCachedResult: Feature0<BuildTask?, BuildTask>,
	hadNoSource: Feature0<BuildTask?, BuildTask>
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

	registerSubjectLessSpec<BuildTask?>(
		"",
		wasInvokedFeature.forSubjectLess(),
		wasInvokedFun.forSubjectLess { isA<BuildTask>() },
		wasNotInvoked.forSubjectLess(),
		wasSuccessful.forSubjectLess(),
		failed.forSubjectLess(),
		wasUpToDate.forSubjectLess(),
		wasSkipped.forSubjectLess(),
		usedCachedResult.forSubjectLess(),
		hadNoSource.forSubjectLess()
	)

	unifySignatures(wasInvokedFeature, wasInvokedFun).forEach { (name, wasInvokedLambda, _) ->
		context(name) {
			test("succeeds if the task was invoked directly") {
				expect {
					expect(successRun.task(":clean")).wasInvokedLambda { isA<BuildTask>() }
				}.notToThrow()
			}

			test("succeeds if the task was invoked transitively") {
				expect {
					expect(successRun.task(":successTask")).wasInvokedLambda { isA<BuildTask>() }
				}.notToThrow()
			}

			test("fails if the task was not invoked") {
				expect {
					expect(successRun.task(":iDontExist")).wasInvokedLambda { isA<BuildTask>() }
				}.toThrow<AssertionError>().messageContains(WAS.getDefault(), INVOKED.getDefault())
			}
		}
	}

	context(wasNotInvoked.name) {
		val wasNotInvokedLambda = wasNotInvoked.lambda

		test("fails if the task was invoked directly") {
			expect {
				expect(successRun.task(":clean")).wasNotInvokedLambda()
			}.toThrow<AssertionError>().messageContains(WAS_NOT.getDefault(), INVOKED.getDefault())
		}

		test("fails if the task was invoked transitively") {
			expect {
				expect(successRun.task(":successTask")).wasNotInvokedLambda()
			}.toThrow<AssertionError>().messageContains(WAS_NOT.getDefault(), INVOKED.getDefault())
		}

		test("succeeds if the task was not invoked") {
			expect {
				expect(successRun.task(":iDontExist")).wasNotInvokedLambda()
			}.notToThrow()
		}
	}

	data class OutcomeCheck(val feature: Feature0<BuildTask?, BuildTask>, val outcome: TaskOutcome, val task: String)

	val outcomeChecks = listOf(
		OutcomeCheck(wasSuccessful, outcome = SUCCESS, task = ":successTask"),
		OutcomeCheck(failed, outcome = FAILED, task = ":failureTask"),
		OutcomeCheck(wasUpToDate, outcome = UP_TO_DATE, task = ":upToDateTask"),
		OutcomeCheck(wasSkipped, outcome = SKIPPED, task = ":skippedTask"),
		OutcomeCheck(usedCachedResult, outcome = FROM_CACHE, ":cachedTask"),
		OutcomeCheck(hadNoSource, outcome = NO_SOURCE, ":noSourceTask")
	)
	outcomeChecks.forEach { (feature, outcome, task) ->
		val featureFun = feature.lambda

		context(feature.name) {
			test("succeeds if the outcome is $outcome") {
				expect {
					expect(outcomeRuns.getValue(outcome).task(task)).featureFun()
				}.notToThrow()
			}

			outcomeChecks.filter { it.outcome != outcome }.forEach { (_, otherOutcome, otherTask) ->
				test("fails if the outcome is $otherOutcome") {
					expect {
						expect(outcomeRuns.getValue(otherOutcome).task(otherTask)).featureFun()
					}.toThrow<AssertionError>().messageContains(outcome.name, otherOutcome.name)
				}
			}

			test("fails if the task was not invoked") {
				expect {
					expect(successRun.task("iDontExist")).featureFun()
				}.toThrow<AssertionError>().messageContains(outcome.name, WAS.getDefault(), INVOKED.getDefault())
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
