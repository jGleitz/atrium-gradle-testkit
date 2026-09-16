plugins {
	kotlin("jvm")
}

dependencies {
	val atriumVersion: String by project

	implementation(project(":translations:${rootProject.name}-translation-en"))
	implementation(gradleTestKit())
	implementation("ch.tutteli.atrium:atrium-core-api:$atriumVersion")
	implementation("ch.tutteli.atrium:atrium-logic:$atriumVersion")
}

var willBePublished: Boolean by extra(true)
