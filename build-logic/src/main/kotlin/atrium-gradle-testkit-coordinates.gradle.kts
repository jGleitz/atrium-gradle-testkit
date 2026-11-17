group = "de.joshuagleitze"

// Artifact version, source identity and remote publication approval are separate inputs.
val requestedVersion = providers.gradleProperty("version").orNull
val stableVersion = """(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"""
val prereleaseIdentifier = """(0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*)"""
val artifactVersion = Regex(
	"""$stableVersion(?:-$prereleaseIdentifier(?:\.$prereleaseIdentifier)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?"""
)
require(requestedVersion == null || artifactVersion.matches(requestedVersion)) {
	"The Gradle property 'version' must be an unprefixed artifact version like '2.0.0' or '2.0.0-dev'."
}
val releaseTag = providers.gradleProperty("releaseTag").orNull
require(releaseTag == null || (
	requestedVersion != null && Regex(stableVersion).matches(requestedVersion) && releaseTag == "v$requestedVersion"
)) {
	"The Gradle property 'releaseTag' must match 'v<version>' with a stable artifact version, but is '$releaseTag'."
}
version = requestedVersion ?: "local"
// Only a release tag, which was validated above, makes a release. Every other build is unreleased, which Gradle's default
// status scheme calls "integration". The publishing convention writes this status into the published module metadata.
status = if (releaseTag != null) "release" else "integration"
