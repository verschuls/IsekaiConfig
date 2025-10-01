package me.verschuls.icfg.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be executed after all configuration fields have been injected.
 * 
 * <p>The annotated method is called automatically by {@link me.verschuls.icfg.ConfigManager ConfigManager}
 * once all {@link IField} annotated fields in the class have been successfully populated
 * from the configuration.
 * 
 * <h3>Usage Rules</h3>
 * <ul>
 *   <li>Only ONE method per class can have this annotation</li>
 *   <li>The method must be parameterless</li>
 *   <li>The method is called after ALL fields are injected, not during</li>
 *   <li>If field injection fails, this method will not be called</li>
 * </ul>
 * 
 * <h3>Common Use Cases</h3>
 * <ul>
 *   <li>Validate field combinations</li>
 *   <li>Initialize derived values</li>
 *   <li>Start services that depend on configuration</li>
 *   <li>Log configuration values</li>
 * </ul>
 * 
 * <h3>Example</h3>
 * <pre>{@code
 * @ConfigInfo(name = "database")
 * public class DatabaseConfig implements IConfig {
 *     @IField(path = "host")
 *     private String host;
 *     
 *     @IField(path = "port")
 *     private Integer port;
 *     
 *     private String connectionUrl;
 *     
 *     @OnInject
 *     private void initialize() {
 *         // Called after host and port are injected
 *         this.connectionUrl = String.format("jdbc:mysql://%s:%d", host, port);
 *         System.out.println("Database configured: " + connectionUrl);
 *     }
 * }
 * }</pre>
 * 
 * @see IField
 * @see ConfigInfo
 * @see me.verschuls.icfg.IConfig IConfig
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnInject {
}
