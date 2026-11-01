package de.joshuagleitze.test.gradle.samples

import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/*
 * The samples use fixed build results instead of running Gradle builds. They are dynamic proxies rather than classes
 * implementing the TestKit interfaces, because TestKit versions differ in their abstract members.
 */

internal fun <T : BuildTask> sampleBuildTask(type: Class<T>, path: String, outcome: TaskOutcome): T =
	testKitProxy(type, "$path=$outcome") { method, _ ->
		when (method.name) {
			"getPath" -> path
			"getOutcome" -> outcome
			else -> unsupported(method)
		}
	}

internal fun sampleBuildTask(path: String, outcome: TaskOutcome): BuildTask =
	sampleBuildTask(BuildTask::class.java, path, outcome)

internal fun <T : BuildResult> sampleBuildResult(type: Class<T>, output: String, tasks: List<BuildTask>): T =
	testKitProxy(type, "BuildResult(tasks=$tasks)") { method, arguments ->
		when (method.name) {
			"getOutput" -> output
			"getTasks" -> tasks
			"task" -> tasks.find { it.path == arguments.single() }
			"tasks" -> tasks.filter { it.outcome == arguments.single() }
			"taskPaths" -> tasks.filter { it.outcome == arguments.single() }.map { it.path }
			else -> unsupported(method)
		}
	}

internal fun sampleBuildResult(output: String, tasks: List<BuildTask>): BuildResult =
	sampleBuildResult(BuildResult::class.java, output, tasks)

internal fun fails(expectation: () -> Unit) {
	expect(expectation).toThrow<AssertionError>()
}

private fun <T> testKitProxy(type: Class<T>, description: String, answer: (Method, List<Any?>) -> Any?): T =
	type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, arguments ->
		when (method.name) {
			"toString" -> description
			"hashCode" -> System.identityHashCode(proxy)
			"equals" -> proxy === arguments?.single()
			else -> answer(method, arguments?.toList() ?: emptyList())
		}
	})

private fun unsupported(method: Method): Nothing = throw UnsupportedOperationException("${method.name} is not part of the sample")
