package de.joshuagleitze.test.gradle.consumer

/*
 * A plain executable consumer of the staged library. Compiling this source set proves that the public API, including
 * its generic signatures (see SignatureProbes.kt), is usable with the probed Kotlin compiler. Running it proves that the
 * expectations behave as specified (see BehaviorProbes.kt). It prints CONSUMER_OK only after every probe behaved as
 * expected; any unexpected behavior ends the program with an exception instead.
 */
fun main() {
	println("CONSUMER_LIBRARY_JAR=${jarOf("de.joshuagleitze.test.gradle.BuildTaskExpectationsKt")}")
	println("CONSUMER_KOTLIN_STDLIB_JAR=${jarOf("kotlin.Unit")}")
	println("CONSUMER_JVM=${System.getProperty("java.version")}")
	runSignatureProbes()
	val checks = runBehaviorProbes()
	println("CONSUMER_CHECKS=$checks")
	println("CONSUMER_OK")
}

private fun jarOf(className: String): String = Class.forName(className).protectionDomain.codeSource.location.toURI().path
