rootProject.name = "build-logic"

dependencyResolutionManagement {
	repositories {
		mavenCentral()
	}
	versionCatalogs {
		create("buildLibs") {
			from(files("../gradle/buildLibs.versions.toml"))
		}
	}
}
