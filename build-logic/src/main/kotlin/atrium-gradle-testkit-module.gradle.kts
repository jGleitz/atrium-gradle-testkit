plugins {
	id("atrium-gradle-testkit-kotlin")
	id("atrium-gradle-testkit-coordinates")
	id("atrium-gradle-testkit-documentation")
	id("atrium-gradle-testkit-publishing")
}

val module = extensions.create<AtriumGradleTestkitModuleExtension>("atriumGradleTestkitModule")

dokka {
	moduleName = module.artifactId
}

mavenPublishing {
	pom {
		val groupId = group.toString()
		name = module.artifactId.map { "$groupId:$it" }
	}
}

// The publishing plugin only accepts the artifact ID eagerly
afterEvaluate {
	mavenPublishing {
		coordinates(artifactId = module.artifactId.get())
	}
}
