plugins {
	kotlin("jvm")
}

dependencies {
	val atriumVersion: String by project

	implementation(gradleTestKit())
	implementation(name = "atrium-core-api", version = atriumVersion, group = "ch.tutteli.atrium")
}

var willBePublished: Boolean by extra(true)
