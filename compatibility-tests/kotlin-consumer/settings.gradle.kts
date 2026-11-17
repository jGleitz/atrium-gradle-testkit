// Standalone consumer of the staged library artifact. It deliberately uses neither the host build's build-logic nor
// its Kotlin and Java properties, so that the Kotlin compiler under test is the only Kotlin compiler involved.
pluginManagement {
	val consumerKotlinVersion = providers.gradleProperty("consumerKotlinVersion").orNull
		?: throw GradleException("Missing required property -PconsumerKotlinVersion (the Kotlin compiler version to probe)")
	repositories {
		gradlePluginPortal()
	}
	plugins {
		id("org.jetbrains.kotlin.jvm") version consumerKotlinVersion
	}
}

val stagedRepository = providers.gradleProperty("stagedRepository").orNull
	?: throw GradleException("Missing required property -PstagedRepository (absolute path of the staged Maven repository)")
check(File(stagedRepository).isAbsolute) { "-PstagedRepository must be an absolute path, but was '$stagedRepository'" }

dependencyResolutionManagement {
	repositories {
		exclusiveContent {
			forRepository {
				maven {
					name = "staged"
					url = uri(stagedRepository)
				}
			}
			filter {
				includeModule("de.joshuagleitze", "atrium-gradle-testkit")
			}
		}
		mavenCentral()
	}
}

rootProject.name = "kotlin-consumer"
