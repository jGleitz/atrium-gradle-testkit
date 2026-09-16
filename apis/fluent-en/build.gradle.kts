plugins {
	kotlin("jvm")
}

dependencies {
	val kotestVersion: String by project
	val atriumVersion: String by project

	api(project(":${rootProject.name}-logic"))
	api(project(":translations:${rootProject.name}-translation-en"))
	implementation(gradleTestKit())
	implementation("ch.tutteli.atrium:atrium-core-api:$atriumVersion")
	implementation("ch.tutteli.atrium:atrium-logic:$atriumVersion")

	testImplementation(project(":api-spec"))
	testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}

kotlin {
	explicitApi()
}

var willBePublished: Boolean by extra(true)
