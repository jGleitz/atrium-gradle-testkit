plugins {
	kotlin("jvm")
}

dependencies {
	val atriumVersion: String by project

	implementation(project(":translations:${rootProject.name}-translation-en"))
	implementation(gradleTestKit())
	implementation(name = "atrium-core-api", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(name = "atrium-logic", version = atriumVersion, group = "ch.tutteli.atrium")
}

var willBePublished: Boolean by extra(true)
