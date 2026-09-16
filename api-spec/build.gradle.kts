plugins {
	kotlin("jvm")
}

dependencies {
	val kotestVersion: String by project
	val atriumVersion: String by project

	api(name = "atrium-specs", version = atriumVersion, group = "ch.tutteli.atrium") {
		exclude(group = "org.spekframework.spek2")
		exclude(group = "ch.tutteli.spek")
	}
	implementation(gradleTestKit())
	implementation(project(":translations:${rootProject.name}-translation-en"))
	implementation(name = "atrium-fluent-en_GB", version = atriumVersion, group = "ch.tutteli.atrium")
	implementation(name = "kotest-runner-junit5", version = kotestVersion, group = "io.kotest")
}
