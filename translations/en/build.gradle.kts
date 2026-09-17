plugins {
	kotlin("jvm")
}

dependencies {
	val atriumVersion: String by project

	implementation(gradleTestKit())
	implementation("ch.tutteli.atrium:atrium-core-api:$atriumVersion")
}

var willBePublished: Boolean by extra(true)
