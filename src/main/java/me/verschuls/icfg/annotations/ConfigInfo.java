package me.verschuls.icfg.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines configuration metadata for a class implementing {@link me.verschuls.icfg.IConfig IConfig}.
 * 
 * <h3>Path Prefixing</h3>
 * When {@code name} is specified, it becomes the base section for all {@link IField} paths in the class:
 * 
 * <pre>{@code
 * @ConfigInfo(name = "database")
 * public class DatabaseConfig implements IConfig {
 *     @IField(path = "host")        // Actual path: "database.host"
 *     private String host;
 *     
 *     @IField(path = "connection.timeout")  // Actual path: "database.connection.timeout"
 *     private Integer timeout;
 * }
 * }</pre>
 * 
 * <h3>Without Section Name</h3>
 * When {@code name} is empty, fields are read from the root level:
 * 
 * <pre>{@code
 * @ConfigInfo()  // or no annotation at all
 * public class ServerConfig implements IConfig {
 *     @IField(path = "port")         // Reads from root: "port"
 *     private Integer port;
 * }
 * }</pre>
 * 
 * @see IField
 * @see me.verschuls.icfg.IConfig
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigInfo {
    /**
     * Base YAML section for all fields in this class.
     * If empty, fields are read from the root level.
     * 
     * @return the section name to prepend to all field paths
     */
    String name() default "";
    
    /**
     * Whether all fields in this section must exist in the configuration.
     * Overrides individual {@link IField#required()} settings when true.
     * 
     * @return {@code true} if all fields are required, {@code false} otherwise
     */
    boolean required() default false;
    
    /**
     * Whether to process all fields in the class, even those without {@link IField}.
     * When true, field names are used as paths.
     * 
     * @return {@code true} to process all fields, {@code false} to process only annotated fields
     */
    boolean allFields() default false;
}
