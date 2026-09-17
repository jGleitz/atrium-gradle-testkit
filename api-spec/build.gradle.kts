plugins {
	kotlin("jvm")
}

dependencies {
	val kotestVersion: String by project
	val atriumVersion: String by project

	api("ch.tutteli.atrium:atrium-specs:$atriumVersion") {
		exclude(group = "org.spekframework.spek2")
		exclude(group = "ch.tutteli.spek")
	}
	implementation(gradleTestKit())
	implementation(project(":translations:${rootProject.name}-translation-en"))
	implementation("ch.tutteli.atrium:atrium-fluent-en_GB:$atriumVersion")
	implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}
