import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.api.publish.PublicationArtifact
import org.gradle.api.publish.internal.PublicationInternal

/**
 * A view on a publication that reports the release identity as its component status instead of a status inferred from
 * the artifact version. Gradle derives `org.gradle.status` in the module metadata from the version number (release for
 * every version that does not end in SNAPSHOT), which does not reflect this project's release identity. Everything
 * else is delegated to the wrapped publication unchanged.
 */
class StatusOverridingPublication<T : PublicationArtifact>(
	private val delegate: PublicationInternal<T>,
	private val statusAttributes: ImmutableAttributes,
) : PublicationInternal<T> by delegate {
	override fun getAttributes(): ImmutableAttributes = statusAttributes
}
