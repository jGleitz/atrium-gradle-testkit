# atrium-gradle-testkit [![CI Status](https://github.com/jGleitz/atrium-gradle-testkit/workflows/CI/badge.svg)](https://github.com/jGleitz/atrium-gradle-testkit/actions) [![Maven Central Version](https://img.shields.io/maven-central/v/de.joshuagleitze/atrium-gradle-testkit-fluent-en?strategy=releaseProperty&logo=sonatype)](https://central.sonatype.com/artifact/de.joshuagleitze/atrium-gradle-testkit-fluent-en) 

[Atrium](https://github.com/robstoll/atrium) assertions to test [Gradle plugins with TestKit](https://docs.gradle.org/current/userguide/test_kit.html).

[:point_right: **Documentation of all assertions**](https://jgleitz.github.io/atrium-gradle-testkit/apis/atrium-gradle-testkit-fluent-en/atrium-gradle-testkit-fluent-en/de.joshuagleitze.test.gradle/)

## Example

Here is how you can use this library to test Gradle plugins.
The example uses [Kotest](https://kotest.io/) with a native temporary directory.
You can see the whole example in the [`example-project` folder](./example-project). 

```kotlin
class KotlinPluginSpec: FunSpec({
	val projectDir = Files.createTempDirectory("atrium-gradle-testkit-example-")
	var setupCompleted = false
	var testsSuccessful = true

	beforeSpec {
		/* set up the Gradle project in projectDir */
		setupCompleted = true
	}

	afterTest { (_, result) ->
		if (result.isErrorOrFailure) {
			testsSuccessful = false
		}
	}

	afterSpec {
		if (setupCompleted && testsSuccessful) {
			check(projectDir.toFile().deleteRecursively()) {
				"Failed to delete temporary project directory: $projectDir"
			}
		}
	}

	context("run") {
		test("compiles the Kotlin code and runs it") {
			val runResult = GradleRunner.create()
				.forwardOutput()
				.withProjectDir(projectDir.toFile())
				.withArguments("run")
				.build()

			expect(runResult) {
				task(":compileKotlin").wasSuccessful()
				task(":classes").wasInvoked()
				output.contains("Hello World!")
			}
		}
	}
})
```

## Compatibility

This library requires:
 * Kotlin ≥ 2.2
 * Java ≥ 17

## [Contributions welcome](http://contributionswelcome.org/)

All contributions (no matter if small) are always welcome.

Applying the [YAGNI principle](https://wikipedia.org/wiki/YAGNI), this library only provides the functionality that was needed by someone.
If you have any idea for how this library could be more useful, please [create an issue](https://github.com/jGleitz/atrium-gradle-testkit/issues/new)!
Ideas for improvements are always welcome.
