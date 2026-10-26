group = "de.joshuagleitze"

// Releases pass their Git tag (for example `v1.2.3`) as the `version` Gradle property.
val releaseTag = version.toString().takeUnless { it == Project.DEFAULT_VERSION }
version = releaseTag?.removePrefix("v") ?: "local"
status = if (releaseTag != null) "release" else "snapshot"
