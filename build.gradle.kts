import de.marcphilipp.gradle.nexus.NexusRepository
import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.4.10"
	id("com.palantir.git-version") version "0.12.3"
	id("org.jetbrains.dokka") version "1.4.10"
	id("de.marcphilipp.nexus-publish") version "0.4.0"
	id("io.codearte.nexus-staging") version "0.22.0"
	`maven-publish`
	signing
}

allprojects {
	repositories {
		jcenter()
	}
}

group = "de.joshuagleitze"
version = if (isSnapshot) versionDetails.gitHash else versionDetails.lastTag.drop("v")
status = if (isSnapshot) "snapshot" else "release"

val ossrhUsername: String? by project
val ossrhPassword: String? by project
val githubRepository: String? by project
val githubOwner = githubRepository?.split("/")?.get(0)
val githubToken: String? by project

nexusStaging {
	username = ossrhUsername
	password = ossrhPassword
	numberOfRetries = 42
}

subprojects {
	group = rootProject.group
	version = rootProject.version
	status = rootProject.status

	afterEvaluate {
		if (plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
			apply {
				plugin("org.jetbrains.dokka")
				plugin("de.marcphilipp.nexus-publish")
				plugin("org.gradle.maven-publish")
				plugin("org.gradle.signing")
			}

			tasks.withType<Test> {
				useJUnitPlatform()
				reports.junitXml.isEnabled = true
			}

			java {
				sourceCompatibility = VERSION_1_8
				targetCompatibility = VERSION_1_8
			}

			tasks.withType<KotlinCompile> {
				kotlinOptions {
					jvmTarget = "1.8"
					// TODO workaround for https://youtrack.jetbrains.com/issue/KT-41142
					freeCompilerArgs += "-Xno-optimized-callable-references"
				}
			}
		}

		if (willBePublished) {
			val sourcesJar by tasks.registering(Jar::class) {
				group = "build"
				description = "Assembles the source code into a jar"
				archiveClassifier.set("sources")
				from(sourceSets.main.get().allSource)
			}

			tasks.withType<DokkaTask> {
				dokkaSourceSets.named("main") {
					sourceLink {
						localDirectory.set(file("src/main/kotlin"))
						remoteUrl.set(uri("https://github.com/$githubRepository/blob/master/src/main/kotlin").toURL())
						remoteLineSuffix.set("#L")
					}
				}
			}

			val dokkaJar by tasks.registering(Jar::class) {
				group = "build"
				description = "Assembles the Kotlin docs with Dokka"
				archiveClassifier.set("javadoc")
				from(tasks.dokkaJavadoc)
			}

			artifacts {
				archives(sourcesJar)
				archives(dokkaJar)
			}

			lateinit var publication: MavenPublication
			lateinit var githubPackages: ArtifactRepository
			lateinit var mavenCentral: NexusRepository

			publishing {
				publications {
					publication = create<MavenPublication>("maven") {
						from(components["java"])
						artifact(sourcesJar)
						artifact(dokkaJar)

						pom {
							name.set(provider { "$groupId:$artifactId" })
							description.set("Atrium assertions for testing Gradle plugins.")
							inceptionYear.set("2020")
							url.set("https://github.com/$githubRepository")
							ciManagement {
								system.set("GitHub Actions")
								url.set("https://github.com/$githubRepository/actions")
							}
							issueManagement {
								system.set("GitHub Issues")
								url.set("https://github.com/$githubRepository/issues")
							}
							developers {
								developer {
									name.set("Joshua Gleitze")
									email.set("dev@joshuagleitze.de")
								}
							}
							scm {
								connection.set("scm:git:https://github.com/$githubRepository.git")
								developerConnection.set("scm:git:git://git@github.com:$githubRepository.git")
								url.set("https://github.com/$githubRepository")
							}
							licenses {
								license {
									name.set("MIT")
									url.set("https://opensource.org/licenses/MIT")
									distribution.set("repo")
								}
							}
						}
					}
				}
				repositories {
					githubPackages = maven("https://maven.pkg.github.com/$githubRepository") {
						name = "GitHubPackages"
						credentials {
							username = githubOwner
							password = githubToken
						}
					}
				}
			}

			nexusPublishing {
				repositories {
					mavenCentral = sonatype {
						username.set(ossrhUsername)
						password.set(ossrhPassword)
					}
				}
			}

			val publishToGithub = tasks.named("publishAllPublicationsTo${githubPackages.name.capitalize()}Repository")
			val publishToMavenCentral = tasks.named("publishTo${mavenCentral.name.capitalize()}")

			signing {
				val signingKey: String? by project
				val signingKeyPassword: String? by project
				useInMemoryPgpKeys(signingKey, signingKeyPassword)
				sign(publication)
			}

			val checkDependenciesBeforePublishing by tasks.registering {
				group = "publishing"
				description = "Checks that all dependencies are also being published"

				doFirst {
					project.configurations.runtimeClasspath {
						allDependencies.withType<ProjectDependency> {
							check(dependencyProject.willBePublished) {
								"This project has a dependency on $dependencyProject, but the latter will not be published!"
							}
						}
					}
				}
			}

			rootProject.tasks.closeAndReleaseRepository { mustRunAfter(publishToMavenCentral) }

			publishToMavenCentral { dependsOn(checkDependenciesBeforePublishing) }
			publishToGithub { dependsOn(checkDependenciesBeforePublishing) }

			task("release") {
				group = "release"
				description = "Releases the project to all remote repositories"
				dependsOn(publishToGithub, publishToMavenCentral, rootProject.tasks.closeAndReleaseRepository)
			}
		}
	}
}

val Project.isSnapshot get() = versionDetails.commitDistance != 0
fun String.drop(prefix: String) = if (this.startsWith(prefix)) this.drop(prefix.length) else this
val Project.versionDetails get() = (this.extra["versionDetails"] as groovy.lang.Closure<*>)() as com.palantir.gradle.gitversion.VersionDetails
val Project.willBePublished get() = this.extra.properties["willBePublished"] as Boolean? ?: false
