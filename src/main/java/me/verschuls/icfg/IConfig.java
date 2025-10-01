package me.verschuls.icfg;

/**
 * Marker interface for configuration classes that can be automatically loaded
 * and managed by the {@link ConfigManager}.
 * 
 * <p>Classes implementing this interface represent configuration objects that
 * can have their fields automatically populated from YAML configuration files.
 * The ConfigManager uses reflection and ClassGraph to introspect these classes
 * and inject values from the configuration file into fields annotated with
 * {@link me.verschuls.icfg.annotations.IField @IField}.
 * 
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * public class DatabaseConfig implements IConfig {
 *     @IField(path = "host")
 *     private String host = "localhost";
 *     
 *     @IField(path = "port")
 *     private int port = 3306;
 *     
 *     @IField(path = "credentials.username")
 *     private String username;
 * }
 * }</pre>
 * 
 * <h2>Nested Configuration</h2>
 * <p>IConfig implementations can be nested within other IConfig classes to create
 * hierarchical configuration structures. When a field's type implements IConfig,
 * the ConfigManager automatically creates an instance and recursively populates it:
 * 
 * <pre>{@code
 * public class MainConfig implements IConfig {
 *     @IField(path = "database")
 *     private DatabaseConfig database;  // Automatically instantiated and populated
 *     
 *     @IField(path = "cache")
 *     private CacheConfig cache;  // Another nested IConfig
 * }
 * }</pre>
 * 
 * <h2>Optional Annotations</h2>
 * 
 * <h3>{@link me.verschuls.icfg.annotations.ConfigInfo @ConfigInfo}</h3>
 * <p>Can be applied to the class to specify configuration metadata such as
 * the root section name in the YAML file:
 * 
 * <pre>{@code
 * @ConfigInfo(name = "database")
 * public class DatabaseConfig implements IConfig {
 *     // Fields will be loaded from the "database" section
 * }
 * }</pre>
 * 
 * <h3>{@link me.verschuls.icfg.annotations.OnInject @OnInject}</h3>
 * <p>Can be applied to methods that should be invoked after all fields have
 * been injected. This is useful for validation or post-processing:
 * 
 * <pre>{@code
 * public class ServerConfig implements IConfig {
 *     @IField(path = "port")
 *     private int port;
 *     
 *     @OnInject
 *     private void validate() {
 *         if (port < 1 || port > 65535) {
 *             throw new IllegalStateException("Invalid port: " + port);
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h2>Constructor Requirements</h2>
 * <p>Classes implementing IConfig must have either:
 * <ul>
 *   <li>A no-argument constructor (can be private)</li>
 *   <li>A single-argument constructor that accepts the configuration value directly</li>
 * </ul>
 * 
 * <p>The ConfigManager uses ClassGraph to detect and invoke the appropriate
 * constructor, even if it's private or protected.
 * 
 * <h2>Thread Safety</h2>
 * <p>IConfig implementations should be treated as immutable after initialization.
 * The ConfigManager populates all fields during the build phase, and they should
 * not be modified afterward to ensure thread safety.
 * 
 * @see ConfigManager
 * @see me.verschuls.icfg.annotations.IField
 * @see me.verschuls.icfg.annotations.ConfigInfo
 * @see me.verschuls.icfg.annotations.OnInject
 * 
 * @author verschuls
 * @since 1.0
 */
public interface IConfig {}