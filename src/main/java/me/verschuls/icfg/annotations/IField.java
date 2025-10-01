package me.verschuls.icfg.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a field to a YAML configuration value.
 * 
 * <h3>Supported Types</h3>
 * <ul>
 *   <li>Primitives: {@link Boolean}, {@link String}, {@link Integer}, {@link Double}, {@link Float}</li>
 *   <li>{@link java.util.List List} - Can contain any of the above types
 *   <li>{@link dev.dejvokep.boostedyaml.block.implementation.Section Section} - For reading YAML sections (requires default resources)</li>
 *   <li>Custom Classes with single Parameter constructor. Parameter type is required to be one of the above</li>
 *   <li>Nested {@link me.verschuls.icfg.IConfig IConfig} objects</li>
 * </ul>
 * 
 * <h3>Example</h3>
 * <pre>{@code
 * @IField(path = "database.host")
 * private String host = "localhost";
 * 
 * @IField(path = "port", required = true)
 * private Integer port;
 * }</pre>
 * 
 * @see me.verschuls.icfg.IConfig
 * @see ConfigInfo
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IField {
    /**
     * YAML path using dot notation (e.g., "database.host").
     * If class has {@link ConfigInfo}, its section name is auto-prepended.
     * 
     * @return the dot-separated path to the configuration value
     */
    String path() default "";

    /**
     * Whether this field must exist in the configuration.
     * Required fields fail loading if missing; optional fields use defaults or null.
     * 
     * @return {@code true} if required, {@code false} if optional (default)
     */
    boolean required() default false;
}
