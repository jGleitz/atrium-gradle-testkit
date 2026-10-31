package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples
import de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples
import io.kotest.core.spec.style.FunSpec
import java.lang.reflect.Modifier

class RunnableSamplesSpec : FunSpec({
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
})
