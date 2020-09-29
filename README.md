# atrium-gradle-testkit [![CI Status](https://github.com/jGleitz/atrium-gradle-testkit/workflows/CI/badge.svg)](https://github.com/jGleitz/atrium-gradle-testkit/actions) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.joshuagleitze/atrium-gradle-teskit-fluent-en/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.joshuagleitze/atrium-gradle-teskit-fluent-en)

[Atrium](https://github.com/robstoll/atrium) assertions to test [Gradle plugins with TestKit](https://docs.gradle.org/current/userguide/test_kit.html).

[:point_right: **Documentation of all assertions**](https://jgleitz.github.io/atrium-gradle-testkit/apis/atrium-gradle-testkit-fluent-en/atrium-gradle-testkit-fluent-en/de.joshuagleitze.test.gradle/)

## Example

Here is how you can use this library to test Gradle plugins.
The example uses [Spek](https://www.spekframework.org/) with [spek-testfiles](https://github.com/jGleitz/spek-testfiles).
You can see the whole example in the [`example-project` folder](./example-project). 

```kotlin
object KotlinPluginSpek: Spek({
	val testFiles = testFiles()
	val projectDir by memoized(SCOPE) { testFiles.createDirectory("testProject") }

	describe("run") {
		it("compiles the Kotlin code and runs it") {
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

	beforeGroup {
		/* set up the gradle project */
	}
})
```

## [Contributions welcome](http://contributionswelcome.org/)

All contributions (no matter if small) are always welcome.

Applying the [YAGNI principle](https://wikipedia.org/wiki/YAGNI), this library only provides the functionality that was needed by someone.
If you have any idea for how this library could be more useful, please [create an issue](https://github.com/jGleitz/atrium-gradle-testkit/issues/new)!
Ideas for improvements are always welcome.
