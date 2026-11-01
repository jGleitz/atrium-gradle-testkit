package de.joshuagleitze.test.gradle.samples

import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import de.joshuagleitze.test.gradle.notToBeInvoked
import de.joshuagleitze.test.gradle.task
import de.joshuagleitze.test.gradle.toBeInvoked
import de.joshuagleitze.test.gradle.toBeSkipped
import de.joshuagleitze.test.gradle.toBeSuccessful
import de.joshuagleitze.test.gradle.toBeUpToDate
import de.joshuagleitze.test.gradle.toHaveBeenLoadedFromCache
import de.joshuagleitze.test.gradle.toHaveFailed
import de.joshuagleitze.test.gradle.toHaveNoSource
import de.joshuagleitze.test.gradle.toHaveOutcome
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class BuildTaskExpectationSamples {
	private val result = sampleBuildResult(
		"",
		listOf(
			sampleBuildTask(":compileKotlin", SUCCESS),
			sampleBuildTask(":test", FAILED),
			sampleBuildTask(":processResources", NO_SOURCE),
			sampleBuildTask(":compileJava", FROM_CACHE),
			sampleBuildTask(":jar", UP_TO_DATE),
			sampleBuildTask(":javadoc", SKIPPED)
		)
	)

	fun toBeInvokedFeature() {
		expect(result).task(":compileKotlin").toBeInvoked().feature(BuildTask::getPath).toEqual(":compileKotlin")

		fails {
			expect(result).task(":publish").toBeInvoked()
		}
	}

	fun toBeInvoked() {
		expect(result).task(":compileKotlin").toBeInvoked {
			feature(BuildTask::getPath).toEqual(":compileKotlin")
		}

		fails {
			expect(result).task(":publish").toBeInvoked {
				feature(BuildTask::getPath).toEqual(":publish")
			}
		}
	}

	fun notToBeInvoked() {
		expect(result).task(":publish").notToBeInvoked()

		fails {
			expect(result).task(":compileKotlin").notToBeInvoked()
		}
	}

	fun toHaveOutcome() {
		expect(result).task(":jar").toHaveOutcome(UP_TO_DATE)

		fails {
			expect(result).task(":jar").toHaveOutcome(SUCCESS)
		}
	}

	fun toBeSuccessful() {
		expect(result).task(":compileKotlin").toBeSuccessful()

		fails {
			expect(result).task(":test").toBeSuccessful()
		}
	}

	fun toHaveFailed() {
		expect(result).task(":test").toHaveFailed()

		fails {
			expect(result).task(":compileKotlin").toHaveFailed()
		}
	}

	fun toBeUpToDate() {
		expect(result).task(":jar").toBeUpToDate()

		fails {
			expect(result).task(":compileKotlin").toBeUpToDate()
		}
	}

	fun toBeSkipped() {
		expect(result).task(":javadoc").toBeSkipped()

		fails {
			expect(result).task(":compileKotlin").toBeSkipped()
		}
	}

	fun toHaveBeenLoadedFromCache() {
		expect(result).task(":compileJava").toHaveBeenLoadedFromCache()

		fails {
			expect(result).task(":compileKotlin").toHaveBeenLoadedFromCache()
		}
	}

	fun toHaveNoSource() {
		expect(result).task(":processResources").toHaveNoSource()

		fails {
			expect(result).task(":compileKotlin").toHaveNoSource()
		}
	}
}
