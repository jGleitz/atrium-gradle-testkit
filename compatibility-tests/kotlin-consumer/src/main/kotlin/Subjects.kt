package de.joshuagleitze.test.gradle.consumer

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/*
 * Controlled subjects. They are dynamic proxies rather than classes implementing the TestKit interfaces, because
 * TestKit versions differ in their abstract members.
 */

interface ConcreteBuildTask : BuildTask

interface ConcreteBuildResult : BuildResult

fun <T : BuildTask> buildTask(type: Class<T>, path: String, outcome: TaskOutcome): T =
	testKitProxy(type, "$path=$outcome") { method, _ ->
		when (method.name) {
			"getPath" -> path
			"getOutcome" -> outcome
			else -> unsupported(method)
		}
	}

fun buildTask(path: String, outcome: TaskOutcome): ConcreteBuildTask =
	buildTask(ConcreteBuildTask::class.java, path, outcome)

fun buildResult(output: String, tasks: List<BuildTask>): ConcreteBuildResult =
	testKitProxy(ConcreteBuildResult::class.java, "BuildResult(tasks=$tasks)") { method, arguments ->
		when (method.name) {
			"getOutput" -> output
			"getTasks" -> tasks
			"task" -> tasks.find { it.path == arguments.single() }
			"tasks" -> tasks.filter { it.outcome == arguments.single() }
			"taskPaths" -> tasks.filter { it.outcome == arguments.single() }.map { it.path }
			else -> unsupported(method)
		}
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

private fun unsupported(method: Method): Nothing = throw UnsupportedOperationException("${method.name} is not part of the probe")
