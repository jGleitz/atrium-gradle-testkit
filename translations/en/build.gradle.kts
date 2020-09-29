plugins {
	kotlin("jvm")
}

dependencies {
	val atriumVersion = "0.13.0"

	implementation(gradleTestKit())
	implementation(name = "atrium-core-api", version = atriumVersion, group = "ch.tutteli.atrium")
}

var willBePublished: Boolean by extra(true)
