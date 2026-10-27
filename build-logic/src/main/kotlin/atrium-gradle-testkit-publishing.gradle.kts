import com.vanniktech.maven.publish.DeploymentValidation
import org.gradle.plugins.signing.Sign

plugins {
	id("atrium-gradle-testkit-coordinates")
	id("atrium-gradle-testkit-kotlin")
	id("com.vanniktech.maven.publish")
}

val githubRepository = providers.gradleProperty("github.repository").get()

// Publishing to a remote repository must be authorized explicitly. Approval alone is not enough: the exact stable
// version and its release tag must match, too. Only Gradle properties without secrets are read here, so that ordinary
// builds never touch credentials. The result is fixed during configuration, so it is part of the configuration cache
// key and the guard needs no project access at execution time.
val remotePublicationDenial: String? = run {
	val approval = providers.gradleProperty("remotePublicationApproved").orNull
	val requestedVersion = providers.gradleProperty("version").orNull
	val releaseTag = providers.gradleProperty("releaseTag").orNull
	val stableVersion = Regex("""(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""")

	val problems = buildList {
		when (approval) {
			"true" -> {}
			null, "false" -> add("the Gradle property 'remotePublicationApproved' is not 'true'")
			else -> add("the Gradle property 'remotePublicationApproved' must be 'true' or 'false', but is '$approval'")
		}
		if (requestedVersion == null || !stableVersion.matches(requestedVersion)) {
			add("the Gradle property 'version' must be a stable version like '1.2.3', but is '$requestedVersion'")
		} else if (releaseTag != "v$requestedVersion") {
			add("the Gradle property 'releaseTag' must be 'v$requestedVersion', but is '$releaseTag'")
		}
	}
	problems.takeIf { it.isNotEmpty() }?.joinToString(
		prefix = "Remote publication is not authorized: ",
		separator = "; ",
		postfix = ". Only an approved release may publish remotely."
	)
}

val checkRemotePublicationAuthorized = tasks.register("checkRemotePublicationAuthorized") {
	group = "publishing"
	description = "Checks that publishing to remote repositories has been authorized explicitly."

	val denial = remotePublicationDenial
	doLast {
		if (denial != null) throw GradleException(denial)
	}
}

/**
 * Lets this task only run if remote publication is authorized. The task depends on the authorization check and
 * repeats it before its own action, so that excluding the check task does not bypass it.
 */
fun Task.requireRemotePublicationAuthorization(isRemote: Provider<Boolean>) {
	dependsOn(isRemote.map { if (it) listOf(checkRemotePublicationAuthorized) else emptyList() })
	val denial = remotePublicationDenial
	doFirst {
		if (isRemote.get() && denial != null) throw GradleException(denial)
	}
}

// Vanniktech’s Central Portal tasks register the upload, release and drop operations that its build service performs
// at the end of the build. Preventing these tasks from running prevents all Central Portal network access.
val mavenCentralTasks = setOf(
	"prepareMavenCentralPublishing",
	"enableAutomaticMavenCentralPublishing",
	"dropMavenCentralDeployment",
	"publishToMavenCentral",
	"publishAndReleaseToMavenCentral",
)
tasks.named { it in mavenCentralTasks }.configureEach {
	requireRemotePublicationAuthorization(provider { true })
}

// Publishing to file repositories, like a local verification repository, needs no authorization.
tasks.withType<PublishToMavenRepository>().configureEach {
	val publishTask = this
	requireRemotePublicationAuthorization(provider {
		publishTask.name.endsWith("ToMavenCentralRepository") || publishTask.repository.url.scheme != "file"
	})
}

// Signing reads the signing key, so it must not happen before the authorization check
tasks.withType<Sign>().configureEach {
	mustRunAfter(checkRemotePublicationAuthorized)
}

publishing {
	repositories {
		maven {
			name = "githubPackages"
			url = uri("https://maven.pkg.github.com/$githubRepository")
			// Gradle resolves the credentials of every repository in the task graph before it runs any task. Requiring
			// them only when authorized keeps the guard the first failure instead of a missing-credentials report.
			if (remotePublicationDenial == null) {
				credentials(PasswordCredentials::class)
			}
		}
	}
}

mavenPublishing {
	// Releasing to Maven Central must be requested explicitly, through `publishAndReleaseToMavenCentral`
	publishToMavenCentral(automaticRelease = false, validateDeployment = DeploymentValidation.PUBLISHED)
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
