plugins {
	id("atrium-gradle-testkit-coordinates")
	id("org.jetbrains.dokka")
	id("org.jetbrains.dokka-javadoc")
}

val githubRepository = providers.gradleProperty("github.repository")
val atriumVersion = versionCatalogs.named("libs").findLibrary("atrium-core").get().get().version

// This is the GitHub Pages site root; only the release workflow deploys its contents.
val pagesSiteRoot = layout.buildDirectory.dir("dokka/html")
dokka.dokkaPublications.named("html") {
	outputDirectory.set(pagesSiteRoot)
}

// The v1 documentation lived below this path of the Pages site. Documentation links in the wild still point there.
val legacyDocumentationEntry = "apis/atrium-gradle-testkit-fluent-en/atrium-gradle-testkit-fluent-en/de.joshuagleitze.test.gradle"

val writeDocumentationRedirects = tasks.register("writeDocumentationRedirects") {
	group = "documentation"
	description = "Adds a static redirect from the entry URL of the v1 documentation to the generated HTML documentation."

	val htmlDirectory = dokka.dokkaPublications.named("html").flatMap { it.outputDirectory }
	val moduleName = dokka.moduleName
	val entryPath = legacyDocumentationEntry
	// The directory belongs to the Dokka task, which cleans it. This task must restore the redirect after every run.
	outputs.upToDateWhen { false }
	doLast {
		val entryDirectory = htmlDirectory.get().asFile.resolve(entryPath)
		val depth = entryPath.count { it == '/' } + 1
		val target = "${"../".repeat(depth)}${moduleName.get()}/de.joshuagleitze.test.gradle/index.html"
		entryDirectory.mkdirs()
		entryDirectory.resolve("index.html").writeText(
			"""
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<title>Redirecting to the atrium-gradle-testkit documentation</title>
			<meta http-equiv="refresh" content="0; url=$target">
			<link rel="canonical" href="$target">
			</head>
			<body>
			<p>The documentation moved. <a href="$target">Continue to the documentation of all assertions.</a></p>
			</body>
			</html>

			""".trimIndent()
		)
	}
}

tasks.named("dokkaGeneratePublicationHtml") {
	finalizedBy(writeDocumentationRedirects)
}

dokka {
	val gitRef = providers.gradleProperty("releaseTag").orElse("main").get()
	val projectPath = projectDir.relativeTo(rootDir).invariantSeparatorsPath
	val projectSourceUrl = githubRepository.map { java.net.URI("https://github.com/$it/blob/$gitRef/$projectPath") }

	dokkaSourceSets.configureEach {
		sourceLink {
			localDirectory = layout.projectDirectory
			remoteUrl = projectSourceUrl
			remoteLineSuffix = "#L"
		}
		externalDocumentationLinks.register("gradle") {
			url("https://docs.gradle.org/current/javadoc/")
			// Gradle’s Javadoc only provides the newer element-list format
			packageListUrl("https://docs.gradle.org/current/javadoc/element-list")
		}
		externalDocumentationLinks.register("atrium") {
			url("https://docs.atriumlib.org/$atriumVersion/kdoc/")
		}
	}
}
