# atrium-gradle-testkit [![CI Status](https://github.com/jGleitz/atrium-gradle-testkit/workflows/CI/badge.svg)](https://github.com/jGleitz/atrium-gradle-testkit/actions) [![Maven Central Version](https://img.shields.io/maven-central/v/de.joshuagleitze/atrium-gradle-testkit?strategy=releaseProperty&logo=sonatype)](https://central.sonatype.com/artifact/de.joshuagleitze/atrium-gradle-testkit)

[Atrium](https://github.com/robstoll/atrium) assertions to test [Gradle plugins with TestKit](https://docs.gradle.org/current/userguide/test_kit.html).

[:point_right: **Documentation of all assertions**](https://jgleitz.github.io/atrium-gradle-testkit/atrium-gradle-testkit/de.joshuagleitze.test.gradle/)

The v2 library uses the single coordinate `de.joshuagleitze:atrium-gradle-testkit`, replacing the
`atrium-gradle-testkit-fluent-en`, `atrium-gradle-testkit-logic` and `atrium-gradle-testkit-translation-en` artifacts.
The Kotlin package remains `de.joshuagleitze.test.gradle`.

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
				task(":compileKotlin").toBeSuccessful()
				task(":classes").toBeInvoked()
				output.toContain("Hello World!")
			}
		}
	}
})
```

## Compatibility

This library requires:
 * Kotlin ≥ 2.2 (the library is compiled with Kotlin language and API version 2.2, so consumers need a compiler that reads these)
 * Java ≥ 21 (the library's bytecode targets Java 21, so the JVM running your tests must be Java 21 or newer)

For contributors: the Gradle launcher JDK (Temurin 27, selected by `mise.toml`) is independent of the JDK toolchain used to compile and test (21, 25 or 27, CI tests all three), the bytecode target (Java 21, `javaReleaseVersion`) and the Kotlin language/API version (2.2, `kotlinReleaseVersion`); the Kotlin compiler itself is the current Kotlin Gradle plugin version from the version catalog.

### Public API reference (ABI)

The public binary API of `:library` is checked in as `library/api/library.api` (Kotlin Gradle plugin's built-in ABI validation).
`./gradlew build` (through `:library:check`) runs `:library:checkKotlinAbi`, which fails if the compiled public API differs from that file; it never rewrites it.
If you change the public API on purpose, run `./gradlew :library:updateKotlinAbi`, review the diff of `library/api/library.api` and commit it together with your change.
This only detects unintended signature changes in this library; it does not check linkage against older versions of its dependencies.

## [Contributions welcome](http://contributionswelcome.org/)

All contributions (no matter if small) are always welcome.

Applying the [YAGNI principle](https://wikipedia.org/wiki/YAGNI), this library only provides the functionality that was needed by someone.
If you have any idea for how this library could be more useful, please [create an issue](https://github.com/jGleitz/atrium-gradle-testkit/issues/new)!
Ideas for improvements are always welcome.
