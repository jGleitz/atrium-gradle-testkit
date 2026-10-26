pluginManagement {
	includeBuild("build-logic")
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
	versionCatalogs {
		create("testLibs") {
			from(files("gradle/testLibs.versions.toml"))
		}
	}
}

rootProject.name = "atrium-gradle-testkit"

include(
	"api-spec",
	"apis:fluent-en",
	"example-project",
	"logic",
	"translations:en"
)
