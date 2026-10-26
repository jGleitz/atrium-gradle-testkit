import org.gradle.api.provider.Property

/**
 * Configures a module of atrium-gradle-testkit that is published.
 */
interface AtriumGradleTestkitModuleExtension {
	/**
	 * The Maven artifact ID that this module is published under.
	 */
	val artifactId: Property<String>
}
