// Standalone consumer build that runs the shared test suite against the staged library, using the TestKit client of the
// Gradle distribution that runs this build. It must never include the host's build logic or root project.

fun requiredProperty(name: String): String =
	providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
		?: throw GradleException("The Gradle property '$name' is required.")

fun requiredAbsolutePath(name: String): File {
	val path = File(requiredProperty(name))
	if (!path.isAbsolute) throw GradleException("The Gradle property '$name' must be an absolute path, but is '$path'.")
	return path
}

val stagedRepository = requiredAbsolutePath("stagedRepository")
requiredProperty("testedLibraryVersion")
requiredProperty("compatibilityGradleVersion")
// Each runtime row has its own build directory. It is set before any build script or plugin can configure tasks. The
// action is isolated, so it must only capture the local value, not the script.
requiredAbsolutePath("compatibilityBuildDir").let { compatibilityBuildDir ->
	gradle.lifecycle.beforeProject {
		layout.buildDirectory = compatibilityBuildDir
	}
}

dependencyResolutionManagement {
	repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
	repositories {
		// The library must come from the staged publication, never from a remote repository or the host's sources.
		exclusiveContent {
			forRepository {
				maven {
					name = "stagedLibrary"
					url = stagedRepository.toURI()
				}
			}
			filter {
				includeModule("de.joshuagleitze", "atrium-gradle-testkit")
			}
		}
		mavenCentral()
	}
	// The host's catalogs own all versions. They are only read; the host's convention plugins are not used.
	versionCatalogs {
		create("libs") {
			from(files("../gradle/libs.versions.toml"))
		}
		create("testLibs") {
			from(files("../gradle/testLibs.versions.toml"))
		}
		create("buildLibs") {
			from(files("../gradle/buildLibs.versions.toml"))
		}
	}
}

rootProject.name = "compatibility-tests"
