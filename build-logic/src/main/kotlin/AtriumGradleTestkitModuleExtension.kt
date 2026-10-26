import org.gradle.api.provider.Property

/**
 * Configures a module of atrium-gradle-testkit that is published.
 */
abstract class AtriumGradleTestkitModuleExtension {
	/**
	 * The Maven artifact ID that this module is published under.
	 */
	abstract val artifactId: Property<String>
}
