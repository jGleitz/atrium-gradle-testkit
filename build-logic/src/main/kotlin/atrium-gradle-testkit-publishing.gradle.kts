import com.vanniktech.maven.publish.DeploymentValidation

plugins {
	id("atrium-gradle-testkit-coordinates")
	id("atrium-gradle-testkit-kotlin")
	id("com.vanniktech.maven.publish")
}

val githubRepository = providers.gradleProperty("github.repository").get()

publishing {
	repositories {
		maven {
			name = "githubPackages"
			url = uri("https://maven.pkg.github.com/$githubRepository")
			credentials(PasswordCredentials::class)
		}
	}
}

mavenPublishing {
	publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)
	signAllPublications()

	pom {
		description = "Atrium assertions for testing Gradle plugins."
		inceptionYear = "2020"
		url = "https://github.com/$githubRepository"
		ciManagement {
			system = "GitHub Actions"
			url = "https://github.com/$githubRepository/actions"
		}
		issueManagement {
			system = "GitHub Issues"
			url = "https://github.com/$githubRepository/issues"
		}
		developers {
			developer {
				name = "Joshua Gleitze"
				email = "dev@joshuagleitze.de"
			}
		}
		scm {
			url = "https://github.com/$githubRepository"
			connection = "scm:git:https://github.com/$githubRepository.git"
			developerConnection = "scm:git:git://git@github.com:$githubRepository.git"
		}
		licenses {
			license {
				name = "MIT"
				url = "https://opensource.org/licenses/MIT"
				distribution = "repo"
			}
		}
	}
}

// A published module must not depend on a project that is not published.
// Published projects offer a marker variant. We request the marker variant of all project dependencies and check that
// each dependency resolved to it. Projects that are not published fail to resolve, or resolve to a variant that does
// not declare any usage.
val publishedModuleUsageName = "atrium-gradle-testkit-published-module"
val publishedModuleUsage = objects.named<Usage>(publishedModuleUsageName)

configurations.consumable("publishedModuleMarker") {
	attributes.attribute(Usage.USAGE_ATTRIBUTE, publishedModuleUsage)
}

val publishedProjectDependencies = configurations.dependencyScope("publishedProjectDependencies") {
	dependencies.addAllLater(
		configurations.runtimeClasspath.map { runtimeClasspath ->
			runtimeClasspath.allDependencies.withType<ProjectDependency>().map(ProjectDependency::copy)
		}
	)
}

val publishedProjectDependencyMarkers = configurations.resolvable("publishedProjectDependencyMarkers") {
	extendsFrom(publishedProjectDependencies.get())
	attributes.attribute(Usage.USAGE_ATTRIBUTE, publishedModuleUsage)
}

val checkProjectDependenciesArePublished = tasks.register("checkProjectDependenciesArePublished") {
	group = "publishing"
	description = "Checks that all projects this project depends on are published, too."

	val dependencyGraph = publishedProjectDependencyMarkers.flatMap { it.incoming.resolutionResult.rootComponent }
	val resolvedUsageAttribute = Attribute.of(Usage.USAGE_ATTRIBUTE.name, String::class.java)
	val markerUsage = publishedModuleUsageName
	doLast {
		val unpublishedDependencies = dependencyGraph.get().dependencies
			.filterNot { dependency ->
				dependency is ResolvedDependencyResult &&
					dependency.resolvedVariant.attributes.getAttribute(resolvedUsageAttribute) == markerUsage
			}
			.map { it.requested.displayName }
		check(unpublishedDependencies.isEmpty()) {
			"This project is published, but depends on projects that are not: ${unpublishedDependencies.joinToString()}"
		}
	}
}

tasks.withType<PublishToMavenRepository>().configureEach {
	dependsOn(checkProjectDependenciesArePublished)
}
