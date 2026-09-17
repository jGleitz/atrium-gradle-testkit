import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.20"
    id("com.palantir.git-version") version "3.4.0"
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    `maven-publish`
    signing
}

val javaReleaseVersion = providers.gradleProperty("javaReleaseVersion").map(JavaLanguageVersion::of)
val javaToolchainVersion = providers.gradleProperty("javaVersion")
    .map(JavaLanguageVersion::of)
    .orElse(javaReleaseVersion)

allprojects {
    repositories {
        mavenCentral()
    }
}

group = "de.joshuagleitze"
val gitRef = if (isSnapshot) versionDetails.gitHash else versionDetails.lastTag
version = if (isSnapshot) gitRef else gitRef.drop("v")
status = if (isSnapshot) "snapshot" else "release"

val ossrhUsername: String? by project
val ossrhPassword: String? by project
val githubRepository: String? by project
val githubOwner = githubRepository?.split("/")?.get(0)
val githubToken: String? by project

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
            username = ossrhUsername
            password = ossrhPassword
        }
    }
    transitionCheckOptions {
        maxRetries = 42
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version
    status = rootProject.status

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = javaToolchainVersion
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release = javaReleaseVersion.map(JavaLanguageVersion::asInt)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            reports.junitXml.required = true
            systemProperty("kotlinVersion", project.getKotlinPluginVersion())
            systemProperty("javaToolchainVersion", javaToolchainVersion.get().toString())
            systemProperty("javaReleaseVersion", javaReleaseVersion.get().toString())
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget = javaReleaseVersion.map { JvmTarget.fromTarget(it.toString()) }
                // TODO workaround for https://youtrack.jetbrains.com/issue/KT-41142
                freeCompilerArgs.add("-Xno-optimized-callable-references")
            }
        }
    }

    afterEvaluate {
        if (willBePublished) {
            apply {
                plugin("org.jetbrains.dokka")
                plugin("org.jetbrains.dokka-javadoc")
                plugin("org.gradle.maven-publish")
                plugin("org.gradle.signing")
            }

            val sourcesJar by tasks.registering(Jar::class) {
                group = "build"
                description = "Assembles the source code into a jar"
                archiveClassifier = "sources"
                from(sourceSets.main.get().allSource)
            }

            dokka {
                dokkaSourceSets.named("main") {
                    sourceLink {
                        val projectPath = projectDir.absoluteFile.relativeTo(rootProject.projectDir.absoluteFile)
                        localDirectory = file("src/main/kotlin")
                        remoteUrl("https://github.com/$githubRepository/blob/$gitRef/$projectPath/src/main/kotlin")
                        remoteLineSuffix = "#L"
                    }
                    externalDocumentationLinks.register("gradle") {
                        url("https://docs.gradle.org/current/javadoc/")
                    }
                    val atriumVersion: String by project
                    externalDocumentationLinks.register("atrium") {
                        url("https://docs.atriumlib.org/$atriumVersion/doc/")
                    }
                }
            }

            val dokkaJar by tasks.registering(Jar::class) {
                group = "build"
                description = "Assembles the Kotlin docs with Dokka"
                archiveClassifier = "javadoc"
                from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
            }

            artifacts {
                archives(sourcesJar)
                archives(dokkaJar)
            }

            lateinit var publication: MavenPublication
            lateinit var githubPackages: ArtifactRepository

            publishing {
                publications {
                    publication = create<MavenPublication>("maven") {
                        from(components["java"])
                        artifact(sourcesJar)
                        artifact(dokkaJar)

                        pom {
                            name = provider { "$groupId:$artifactId" }
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
                                connection = "scm:git:https://github.com/$githubRepository.git"
                                developerConnection = "scm:git:git://git@github.com:$githubRepository.git"
                                url = "https://github.com/$githubRepository"
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

            val publishToGithub =
                tasks.named("publishAllPublicationsTo${githubPackages.name.replaceFirstChar { it.uppercase() }}Repository")
            val publishToSonatype = tasks.named("publishToSonatype")

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
                    val root = project.rootProject
                    project.configurations.runtimeClasspath {
                        allDependencies.withType<ProjectDependency> {
                            val dependencyProject = root.project(path)
                            check(dependencyProject.willBePublished) {
                                "This project has a dependency on $dependencyProject, but the latter will not be published!"
                            }
                        }
                    }
                }
            }

            tasks.register("release") {
                group = "release"
                description = "Releases the project to all remote repositories"
                dependsOn(publishToGithub, publishToSonatype, ":closeAndReleaseSonatypeStagingRepository")
            }
        }
    }
}

gradle.projectsEvaluated {
    val publishedProjects = subprojects.filter { it.willBePublished }
    val publishToSonatype = listOf(tasks.named("publishToSonatype")) +
            publishedProjects.map { it.tasks.named("publishToSonatype") }
    val closeAndReleaseSonatypeStagingRepository = tasks.named("closeAndReleaseSonatypeStagingRepository")
    val dependencyChecks = publishedProjects
        .map { it.tasks.named("checkDependenciesBeforePublishing") }

    tasks.named("initializeSonatypeStagingRepository").configure { dependsOn(dependencyChecks) }
    publishToSonatype.forEach { it.configure { dependsOn(dependencyChecks) } }
    closeAndReleaseSonatypeStagingRepository.configure { mustRunAfter(publishToSonatype) }
    publishedProjects.forEach {
        listOf(
            it.tasks.named("publishMavenPublicationToSonatypeRepository"),
            it.tasks.named("publishMavenPublicationToGitHubPackagesRepository"),
            it.tasks.named("publishAllPublicationsToGitHubPackagesRepository")
        ).forEach { publicationTask ->
            publicationTask.configure { dependsOn(dependencyChecks) }
        }
    }
}

val Project.isSnapshot get() = versionDetails.commitDistance != 0
fun String.drop(prefix: String) = if (this.startsWith(prefix)) this.drop(prefix.length) else this
val Project.versionDetails get() = (this.extra["versionDetails"] as groovy.lang.Closure<*>)() as com.palantir.gradle.gitversion.VersionDetails
val Project.willBePublished get() = this.extra.properties["willBePublished"] as Boolean? ?: false
