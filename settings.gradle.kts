rootProject.name = "atrium-gradle-testkit"

include(
	"api-spec",
	"apis:fluent-en",
	"example-project",
	"logic",
	"translations:en"
)

project(":apis:fluent-en").name = "${rootProject.name}-fluent-en"
project(":logic").name = "${rootProject.name}-logic"
project(":translations:en").name = "${rootProject.name}-translation-en"
