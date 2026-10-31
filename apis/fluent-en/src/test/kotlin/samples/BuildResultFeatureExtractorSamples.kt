package de.joshuagleitze.test.gradle.samples

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import de.joshuagleitze.test.gradle.notToBeInvoked
import de.joshuagleitze.test.gradle.output
import de.joshuagleitze.test.gradle.task
import de.joshuagleitze.test.gradle.toBeInvoked
import de.joshuagleitze.test.gradle.toBeSuccessful
import org.gradle.testkit.runner.TaskOutcome.SUCCESS

class BuildResultFeatureExtractorSamples {
	private val result = sampleBuildResult("Hello World!", listOf(sampleBuildTask(":compileKotlin", SUCCESS)))

	fun outputFeature() {
		expect(result).output.toContain("Hello World!")

		fails {
			expect(result).output.toContain("Goodbye")
		}
	}

	fun output() {
		expect(result).output {
			toContain("Hello")
			toContain("World")
		}

		fails {
			expect(result).output {
				toContain("Goodbye")
			}
		}
	}

	fun taskFeature() {
		expect(result).task(":compileKotlin").toBeSuccessful()
		// a task that the build did not invoke is null
		expect(result).task(":test").toEqual(null)

		fails {
			expect(result).task(":test").toBeInvoked()
		}
	}

	fun task() {
		expect(result)
			.task(":compileKotlin") { toBeSuccessful() }
			.task(":test") { notToBeInvoked() }

		fails {
			expect(result).task(":compileKotlin") {
				notToBeInvoked()
			}
		}
	}
}
