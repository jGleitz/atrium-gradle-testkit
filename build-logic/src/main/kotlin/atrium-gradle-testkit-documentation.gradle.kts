plugins {
	id("atrium-gradle-testkit-coordinates")
	id("org.jetbrains.dokka")
	id("org.jetbrains.dokka-javadoc")
}

val githubRepository = providers.gradleProperty("github.repository")
val atriumVersion = versionCatalogs.named("libs").findLibrary("atrium-core-api").get().get().version

dokka {
	val gitRef = if (status == "release") "v$version" else "main"
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
			url("https://docs.atriumlib.org/$atriumVersion/doc/")
		}
	}
}
