package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.notToThrow
import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import de.joshuagleitze.test.gradle.probes.runGenericSignatureProbes
import de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples
import de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples
import de.joshuagleitze.test.gradle.samples.sampleBuildResult
import de.joshuagleitze.test.gradle.samples.sampleBuildTask
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import java.lang.reflect.Modifier

public class ExpectationContractsSpec : FunSpec({
	val result = sampleBuildResult("Hello World!", listOf(sampleBuildTask(":compileKotlin", SUCCESS)))

	context("samples") {
		val samples = listOf(BuildResultFeatureExtractorSamples(), BuildTaskExpectationSamples())
		val sampleMethods = samples.flatMap { sample ->
			sample.javaClass.declaredMethods.filter { Modifier.isPublic(it.modifiers) }.map { sample to it }
		}

		test("there is one sample for every public operation") {
			expect(sampleMethods.map { (_, method) -> method.name }.sorted()).toEqual(
				listOf(
					"notToBeInvoked", "output", "outputFeature", "task", "taskFeature", "toBeInvoked",
					"toBeInvokedFeature", "toBeSkipped", "toBeSuccessful", "toBeUpToDate", "toHaveBeenLoadedFromCache",
					"toHaveFailed", "toHaveNoSource", "toHaveOutcome"
				)
			)
		}

		sampleMethods.forEach { (sample, method) ->
			test("sample ${sample.javaClass.simpleName}.${method.name} runs") {
				method.invoke(sample)
			}
		}
	}

	test("the generic signature probes hold") {
		runGenericSignatureProbes()
	}

	context("reporting contracts") {
		test("a missing task is a present null subject that permits notToBeInvoked") {
			expect {
				expect(result).task(":missing").toEqual(null)
				expect(result).task(":missing").notToBeInvoked()
				expect(result).task(":missing") { notToBeInvoked() }
			}.notToThrow()
		}

		test("an absent upstream subject does not permit notToBeInvoked") {
			expect {
				expect(result).task(":missing") {
					toBeInvoked().notToBeInvoked()
				}
			}.toThrow<AssertionError>().messageToContain("task ':missing': null", "was", "invoked", "was not")
		}

		test("toBeInvoked reports a missing task as null") {
			expect {
				expect(result).task(":missing").toBeInvoked()
			}.toThrow<AssertionError>().messageToContain("task ':missing': null", "was", "invoked")
		}

		test("notToBeInvoked reports the actual invoked task") {
			expect {
				expect(result).task(":compileKotlin").notToBeInvoked()
			}.toThrow<AssertionError>().messageToContain("task ':compileKotlin'", ":compileKotlin=SUCCESS", "was not", "invoked")
		}

		test("output reports the actual output") {
			expect {
				expect(result).output.toContain("Goodbye")
			}.toThrow<AssertionError>().messageToContain("output:", "Hello World!", "Goodbye")
		}

		test("output { } reports the actual output") {
			expect {
				expect(result).output { toContain("Goodbye") }
			}.toThrow<AssertionError>().messageToContain("output:", "Hello World!", "Goodbye")
		}

		test("task(path) { } reports the actual invoked task") {
			expect {
				expect(result).task(":compileKotlin") { notToBeInvoked() }
			}.toThrow<AssertionError>().messageToContain("task ':compileKotlin'", ":compileKotlin=SUCCESS", "was not", "invoked")
		}

		test("toBeInvoked { } reports a missing task as null") {
			expect {
				expect(result).task(":missing").toBeInvoked { toBeSuccessful() }
			}.toThrow<AssertionError>().messageToContain("task ':missing': null", "was", "invoked", "outcome", SUCCESS.name)
		}

		test("output { } fails for an empty assertionCreator") {
			expect {
				expect(result).output { }
			}.toThrow<AssertionError>().messageToContain("output:", "at least one expectation defined")
		}

		test("task(path) { } fails for an empty assertionCreator") {
			expect {
				expect(result).task(":compileKotlin") { }
			}.toThrow<AssertionError>().messageToContain("task ':compileKotlin'", "at least one expectation defined")
		}

		test("toBeInvoked { } fails for an empty assertionCreator") {
			expect {
				expect(result).task(":compileKotlin").toBeInvoked { }
			}.toThrow<AssertionError>().messageToContain("task ':compileKotlin'", "at least one expectation defined")
		}

		test("toHaveOutcome reports the expected and the actual outcome") {
			expect {
				expect(result).task(":compileKotlin").toHaveOutcome(FAILED)
			}.toThrow<AssertionError>().messageToContain("outcome:", SUCCESS.name, FAILED.name)
		}

		test("an outcome convenience reports the expected and the actual outcome") {
			expect {
				expect(result).task(":compileKotlin").toHaveFailed()
			}.toThrow<AssertionError>().messageToContain("outcome:", SUCCESS.name, FAILED.name)
		}

		test("toHaveOutcome for a missing task reports the invocation and the expected outcome") {
			expect {
				expect(result).task(":missing").toHaveOutcome(SUCCESS)
			}.toThrow<AssertionError>().messageToContain("task ':missing': null", "was", "invoked", "outcome", SUCCESS.name)
		}

		test("feature extraction with callable references to Java getters reports the getter names") {
			expect {
				expect(result).task(":compileKotlin").toBeInvoked {
					feature(BuildTask::getPath).toEqual(":other")
					feature(BuildTask::getOutcome).toEqual(FAILED)
				}
			}.toThrow<AssertionError>().messageToContain("getPath", ":other", "getOutcome", FAILED.name)
		}

		test("task(path) { } reports all failing expectations of its assertionCreator") {
			expect {
				expect(result).task(":compileKotlin") {
					toHaveFailed()
					notToBeInvoked()
				}
			}.toThrow<AssertionError>().messageToContain("task ':compileKotlin'", "outcome:", FAILED.name, "was not")
		}
	}
})
