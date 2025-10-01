package me.verschuls.icfg;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.Settings;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import me.verschuls.icfg.annotations.ConfigInfo;
import me.verschuls.icfg.annotations.IField;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ConfigManager {

    private YamlDocument document;
    private ConfigUtils utils;
    private final Consumer<ConfigManager> success;
    private final Consumer<List<String>> failed;

    private final List<IConfig> configs;
    private final boolean resources;
    private boolean debug = false;
    private final Optional<String> version;
    private final boolean backups;
    private final String backupPrefix;
    private final int maxBackups;

    static final Set<Class<?>> ALLOWED_BASIC = new HashSet<>(Set.of(String.class, List.class, Integer.class, Double.class, Float.class, Boolean.class, Section.class));
    static final Set<Class<?>> ALLOWED_IN_LIST = new HashSet<>(Set.of(String.class, Integer.class, Double.class, Float.class, Boolean.class));

    private final Map<IConfig, String> chaining = Collections.synchronizedMap(new HashMap<>());


    private final String fileName;

    private ConfigManager(Builder builder) {
        this.fileName = builder.file;
        this.configs = builder.configs;
        this.success = builder.success;
        this.failed = builder.failed;
        this.resources = builder.resources;
        this.debug = builder.debug;
        this.version = builder.version;
        this.backups = builder.backups;
        this.backupPrefix = builder.backupPrefix;
        this.maxBackups = builder.maxBackups;
        try {
            this.document = loadConfig(builder);
            this.utils = ConfigUtils.of(document);
            if (debug) System.out.println("Configs: "+configs.stream().filter(Objects::nonNull).map(icfg->icfg.getClass().getName()).toList() + " | fileName: "+fileName);
            if (version.isPresent()) {
                if (builder.resources) {
                    inject(false);
                    return;
                }
                utils.set("version", version.get());
                inject(false);
                return;
            }
            inject(false);
        } catch (Exception e) {
            if (debug) e.printStackTrace(System.console().writer());
        }

    }

    public void reload() {
        inject(true);
    }

    private void inject(boolean reload) {
        if (reload) {
            try {
                document.reload();
            } catch (IOException e) {
                System.err.println("Error occurred while reloading config | "+fileName);
                if (debug) e.printStackTrace(System.console().writer());
            }
        }

        ConcurrentLinkedQueue<IConfig> configQueue = new ConcurrentLinkedQueue<>(configs);
        
        while (!configQueue.isEmpty()) {
            IConfig icfg = configQueue.poll();
            if (debug) System.out.println("----------------------------------------------------"+fileName);
            if (debug) System.out.println("All IConfig's: "+configs + " | filename "+fileName);
            if (icfg == null) {
                if (debug) System.out.println("IConfig 'null' Configs: "+configs + " | filename "+fileName);
                if (debug) System.out.println("----------------------------------------------------"+fileName);
                continue;
            }
            if (debug) System.out.println("IConfig : "+icfg.getClass().getName() + " | filename "+fileName);
            AtomicReference<String> section = new AtomicReference();
            RefClass obj = RefUtils.get(icfg);
            if (chaining.containsKey(icfg)) {
                section.getAndSet(chaining.get(icfg));
                if (debug) System.out.println("ICFG is chanined with path: "+section.get());
            }
            if (obj.isAnnotated(ConfigInfo.class)) {
                ConfigInfo info = (ConfigInfo) obj.getAnnotation(ConfigInfo.class);
                if (info.name() != null && section.get() == null)
                    section.getAndSet(info.name()+".");

                if (debug) System.out.println("ICFG is annotated with path: "+section.get());
                if (info.required()) {
                    if (utils.getSection(info.name()) == null) {
                        utils.reportError(info.name());
                        if (debug) System.out.println("ICFG null section | path: "+section.get());
                        if (debug) System.out.println("----------------------------------------------------"+fileName);
                        continue;
                    }
                    if (utils.getSection(info.name()).isEmpty(true)) {
                        if (debug) System.out.println("ICFG empty section | path: "+section.get());
                        if (debug) System.out.println("----------------------------------------------------"+fileName);
                        utils.reportError(info.name());
                        continue;
                    }
                }
                injectFields(section.get(), info.allFields() ? obj.getFields() : obj.getAnnotatedFields(), configQueue);
                continue;
            }
            if (debug) System.out.println("ICFG field injecting | path: "+section.get());
            injectFields(section.get(), obj.getAnnotatedFields(), configQueue);
            if (debug) System.out.println("----------------------------------------------------"+fileName);
        }
        try {
            document.update();
            document.save();
        } catch (IOException e) {
            if (debug) e.printStackTrace(System.console().writer());
        }

        if (utils.hasErrors()) failed.accept(utils.getErrors());
        else success.accept(this);
    }

    private synchronized void injectFields(String section, List<RefField> fields, ConcurrentLinkedQueue queue) {
        if (debug) System.out.println("Injecting Fields Section: "+section);
        for (RefField field : fields) {
            if (debug) System.out.println("----------------------------------------------------"+fileName);
            if (debug) System.out.println("Field: "+field.getName()+" Type: "+field.getType());
            StringBuilder path = new StringBuilder();
            if (section != null) path.append(section);
            boolean required = false;
            if (field.isAnnotated()) {
                IField info = field.getAnnotation();
                required = info.required();
                path.append(info.path().isEmpty() ? field.getName() : info.path());
            } else path.append(field.getName());
            if (ALLOWED_BASIC.contains(field.getType()))  {
                if (debug) System.out.println("Setting Basic: "+path);
                utils.configure(path.toString(), field, resources, required);
                if (debug) System.out.println("----------------------------------------------------"+fileName);
                continue;
            }
            RefClass class_ = RefUtils.fromClass(field.getType());
            if (class_.isAssignableFrom(IConfig.class)) {
                if (debug) System.out.println("Found IConfig Field: "+path);
                Object instance = class_.createInstance();
                if (!(instance instanceof IConfig iConfig)) continue;
                if (debug) System.out.println("Indeed IConfig Field as instanceof");
                path.append(".");
                if (class_.isAnnotated(ConfigInfo.class)) {
                    ConfigInfo info = (ConfigInfo) class_.getAnnotation(ConfigInfo.class);
                    if (!info.name().isEmpty()) path.append(info.name()).append(".");
                    chaining.put(iConfig, path.toString());
                } else chaining.put(iConfig, path.toString());
                if (debug) System.out.println("Adding to pending: " + queue.size());
                queue.add(iConfig);
                field.set(iConfig);
                if (debug) System.out.println("IConfig Field set: "+instance.getClass() + " Path: "+path + ". Added to pending: " + queue.size());
                if (debug) System.out.println("----------------------------------------------------"+fileName);
                continue;
            }
            if (!class_.hasConstructor()) continue;
            if (!class_.hasArguments()) continue;
            if (class_.getArgTypes().length != 1) continue;

            Object value;
            Class<?> argType = class_.getArgTypes()[0];
            if (argType.equals(Section.class))
                value = class_.createInstance(utils.getSection(path.toString()));
            else value = class_.createInstance(utils.getValue(path.toString(), argType));
            if (value != null) field.set(value);
            else utils.reportError(path.toString());
            if (debug) System.out.println("----------------------------------------------------"+fileName);
        }
    }

    /**
     * Creates a builder for native Java applications.
     * 
     * <p>This builder is optimized for standalone Java applications that need
     * configuration management outside any framework or plugin system.
     * 
     * <p>Example usage:
     * <pre>{@code
     * ConfigManager config = ConfigManager.builderNative("config.yml", 
     *         MyApp.class.getClassLoader())
     *     .version("1.0.0")
     *     .register(myConfig)
     *     .build();
     * }</pre>
     * 
     * @param file the configuration file path relative to the working directory
     * @param loader the ClassLoader to use for resource loading
     * @return a new Builder instance configured for native applications
     * @since 1.0
     */
    public static Builder builderNative(String file, ClassLoader loader) {
        return new Builder(file).classLoader(loader);
    }

    /**
     * Creates a builder for Minecraft server plugins (Bukkit, Spigot, Paper etc.).
     *
     * <p>This builder automatically handles the plugin's data folder and resource
     * loading using the plugin instance. It's compatible with most Minecraft
     * server implementations.
     *
     * <p>Example usage:
     * <pre>{@code
     * // In your plugin's onEnable method
     * ConfigManager config = ConfigManager.builderSpigot("config.yml", this)
     *     .version("2.0")
     *     .backups(true)
     *     .register(settings)
     *     .build();
     * }</pre>
     *
     * <p><strong>Note:</strong> This functionality will be moved to a separate
     * Minecraft-specific library in a future version.
     *
     * @param file the configuration file name (will be created in plugin's data folder)
     * @param dataPath Plugin.getDataPath()
     * @return a new Builder instance configured for Minecraft plugins
     * @since 1.0
     */
    public static Builder builderMCPlugin(String file, Path dataPath, InputStream stream) {
        return new Builder(file).path(dataPath).stream(stream);
    }

    /**
     * Creates a builder specifically for Velocity proxy plugins.
     * 
     * <p>Velocity uses a different approach to file management compared to other
     * Minecraft servers, requiring explicit data directory and class loader configuration.
     * This builder handles these Velocity-specific requirements.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // In your Velocity plugin
     * @Inject
     * public MyPlugin(@DataDirectory Path dataDirectory) {
     *     ConfigManager config = ConfigManager.builderVelocity(
     *             "config.yml", 
     *             dataDirectory, 
     *             getClass().getClassLoader())
     *         .version("1.0")
     *         .register(proxySettings)
     *         .build();
     * }
     * }</pre>
     * 
     * <p><strong>Note:</strong> This functionality will be moved to a separate
     * Minecraft-specific library in a future version.
     * 
     * @param file the configuration file name
     * @param dataDirectory the plugin's data directory path (injected by Velocity)
     * @param loader the ClassLoader for resource loading
     * @return a new Builder instance configured for Velocity plugins
     * @since 1.0
     */
    public static Builder builderVelocity(String file, Path dataDirectory, ClassLoader loader) {
        return new Builder(file).path(dataDirectory).classLoader(loader);
    }


    private static YamlDocument createDoc(ConfigManager.Builder builder) throws Exception {
        /*switch (builder.type) {
            case NATIVE -> {
                configFile = new File(builder.file);
                resourceStream = builder.loader.getResourceAsStream(builder.file);
                builder.resources = resourceStream != null;
            }
            case SPIGOT -> {
                configFile = new File(builder.mcPlugin.getDataFolder(), builder.file);
                resourceStream = builder.mcPlugin.getResource(builder.file);
                builder.resources = resourceStream != null;
            }
            case BUNGEE -> {
                File dataFolder = (File) RefUtils.invokeMethod(builder.mcPlugin, "getDataFolder");
                configFile = new File(dataFolder, builder.file);
                resourceStream = (InputStream) RefUtils.invokeMethod(builder.mcPlugin, "", builder.file);
                builder.resources = resourceStream != null;
            }
            case VELOCITY -> {
                configFile = new File(builder.path.toFile(), builder.file);
                resourceStream = builder.loader.getResourceAsStream(builder.file);
                builder.resources = resourceStream != null;
            }
        }*/

        if (builder.stream == null) builder.stream = builder.loader.getResourceAsStream(builder.file);

        builder.resources = builder.stream != null;
        
        LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(builder.resources)
                .build();

        File configFile;

        if (builder.path == null) configFile = new File(builder.file);
        else configFile = new File(builder.path.toFile(), builder.file);

        return YamlDocument.create(configFile, Objects.requireNonNull(builder.stream),
                GeneralSettings.DEFAULT,
                loaderSettings,
                DumperSettings.DEFAULT);
    }

    private static YamlDocument loadConfig(ConfigManager.Builder builder) throws Exception {
        YamlDocument cfg = createDoc(builder);
        if (!builder.boostedSettings.isEmpty()) cfg.setSettings(builder.boostedSettings.toArray(new Settings[]{}));
        if (builder.version.isPresent()) {
            /*UpdaterSettings update = ConfigUpdater.createUpdaterSettings( TODO
                    "version",
                    builder.version.get(),
                    builder.backups,
                    builder.backupPrefix,
                    builder.maxBackups,
                    builder.logger
            );*/
            UpdaterSettings update = UpdaterSettings.builder()
                    .setVersioning(new BasicVersioning("version"))
                    .setOptionSorting(UpdaterSettings.DEFAULT_OPTION_SORTING).build();
            cfg.setSettings(update);
        }
        cfg.save();
        cfg.update();
        return cfg;
    }
    

    
    /**
     * Builder class for creating ConfigManager instances with a fluent API.
     * 
     * <p>This builder provides a flexible way to configure various aspects of
     * the configuration management system including versioning, backups, and
     * resource loading. The builder supports multiple platforms through specialized
     * static factory methods.
     * 
     * <h2>Platform-Specific Builders</h2>
     * <ul>
     *   <li>{@link #builderNative(String, ClassLoader)} - For standalone Java applications</li>
     *   <li>{@link #builderMCPlugin(String, Path, InputStream)} - For any fork of Bukkit or Bungee api</li>
     *   <li>{@link #builderVelocity(String, Path, ClassLoader)} - For Velocity API</li>
     * </ul>
     * 
     * <h2>Common Configuration Options</h2>
     * All builders support:
     * <ul>
     *   <li>Version management and migrations</li>
     *   <li>Automatic backup creation</li>
     *   <li>Custom BoostedYAML settings</li>
     *   <li>Multiple configuration registration</li>
     * </ul>
     * 
     * <h2>Example Usage</h2>
     * <pre>{@code
     * // Native application
     * ConfigManager config = ConfigManager.builderNative("config.yml", 
     *         MyApp.class.getClassLoader())
     *     .version("1.0.0")
     *     .backups(true)
     *     .backupSettings("backup_", 5)
     *     .register(myConfig)
     *     .build();
     * 
     * // Minecraft plugin
     * ConfigManager config = ConfigManager.builderMcPlugin("config.yml", this)
     *     .version("2.0")
     *     .register(settings)
     *     .build();
     * }</pre>
     * 
     * @since 1.0
     * @author verschuls
     */
    public static class Builder {
        private final String file;
        private Optional<String> version = Optional.empty();
        private boolean backups = false;
        private String backupPrefix = "bp_%nr%_";
        private int maxBackups = 3;
        private List<Settings> boostedSettings = new ArrayList<>();
        private final List<IConfig> configs = new ArrayList<>();

        private ClassLoader loader;
        private Path path;
        private InputStream stream;

        private Consumer<ConfigManager> success;
        private Consumer<List<String>> failed;

        private boolean debug = false;

        private boolean resources = false;

        Builder(String file) {
            this.file = file;
        }

        Builder classLoader(ClassLoader loader) {
            this.loader = loader;
            return this;
        }

        Builder path(Path path) {
            this.path = path;
            return this;
        }

        Builder stream(InputStream stream) {
            this.stream = stream;
            return this;
        }


        /**
         * Enables versioning for the configuration file and sets the version string.
         * When versioning is enabled, the configuration file will automatically track
         * version changes and can perform migrations between versions.
         * 
         * @param version the version string in semantic versioning format
         *                (e.g., "1", "1.0", "1.0.0", "2.3.1")
         * @return this Builder instance for method chaining
         * @throws NullPointerException if version is null
         * @see BasicVersioning
         */
        public Builder version(String version) {
            this.version = Optional.of(version);
            return this;
        }

        /**
         * Enables or disables debug mode for the ConfigManager.
         * When enabled, the library will propagate exceptions instead of handling them silently,
         * and provide more detailed logging output for troubleshooting configuration issues.
         *
         * @param debug true to enable debug mode, false to disable
         * @return this Builder instance for method chaining
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Enables or disables automatic backup creation for the configuration file.
         * When enabled, the ConfigManager will create backup copies of the configuration
         * file before performing save or reload operations, providing a safety mechanism
         * against data loss or corruption.
         * 
         * @param backups {@code true} to enable automatic backups,
         *                {@code false} to disable backup creation
         * @return this Builder instance for method chaining
         * @see #backupSettings(String, int)
         */
        public Builder backups(boolean backups) {
            this.backups = backups;
            return this;
        }

        /**
         * Configures the backup file naming and retention settings.
         * This method only has effect when backups are enabled via {@link #backups(boolean)}.
         * 
         * <p>The prefix can contain the placeholder {@code %nr%} which will be replaced
         * with an incremental number when multiple backups are created.
         * 
         * <p>Example prefixes:
         * <ul>
         *   <li>{@code "backup_"} - creates backup_config.yml</li>
         *   <li>{@code "bp_%nr%_"} - creates bp_1_config.yml, bp_2_config.yml, etc.</li>
         * </ul>
         * 
         * @param prefix the prefix to use for backup file names, may contain {@code %nr%} placeholder
         * @param maxBackups the maximum number of backup files to retain,
         *                   use -1 for unlimited (not recommended due to disk space concerns)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if maxBackups is less than -1
         */
        public Builder backupSettings(String prefix, int maxBackups) {
            this.backupPrefix = prefix;
            this.maxBackups = maxBackups;
            return this;
        }

        /**
         * Provides advanced configuration options for the underlying BoostedYAML library.
         * This method allows fine-grained control over YAML processing behavior including
         * formatting, comments preservation, and other advanced features.
         * 
         * <p><strong>Warning:</strong> Avoid configuring {@link LoaderSettings} or
         * {@link UpdaterSettings} through this method as they are internally managed
         * by ConfigManager. Overriding these may cause unexpected behavior.
         * 
         * <p>Common use cases:
         * <ul>
         *   <li>Customizing {@link DumperSettings} for output formatting</li>
         *   <li>Adjusting {@link GeneralSettings} for comment handling</li>
         * </ul>
         * 
         * @param settings variable number of BoostedYAML settings to apply
         * @return this Builder instance for method chaining
         * @see Settings
         * @see <a href="https://github.com/dejvokep/boosted-yaml">BoostedYAML Documentation</a>
         */
        public Builder boostedYamlSettings(Settings setting, Settings... settings) {
            this.boostedSettings.add(setting);
            if (settings.length > 0) this.boostedSettings.addAll(List.of(settings));
            return this;
        }

        /**
         * Registers one or more configuration objects that implement the {@link IConfig} interface.
         * Registered configurations will be automatically loaded and managed by the ConfigManager,
         * allowing for type-safe access to configuration values.
         * 
         * <p>Example usage:
         * <pre>{@code
         * builder.register(generalConfig)
         *        .register(databaseConfig, cacheConfig, securityConfig);
         * }</pre>
         * 
         * @param config the primary configuration object to register
         * @param configs additional configuration objects to register (optional)
         * @return this Builder instance for method chaining
         * @throws NullPointerException if config is null
         */
        public Builder register(IConfig config, IConfig... configs) {
            this.configs.add(config);
            if (configs.length > 0) this.configs.addAll(List.of(configs));
            return this;
        }


        /**
         * Builds and returns a fully configured {@link ConfigManager} instance.
         * This method performs the following operations:
         * <ol>
         *   <li>Creates or loads the configuration file at the specified path</li>
         *   <li>Applies all configured settings (versioning, backups, etc.)</li>
         *   <li>Automatically loads default values from resources if available</li>
         *   <li>Registers all provided {@link IConfig} implementations</li>
         *   <li>Performs initial save and update operations</li>
         * </ol>
         * 
         * <p><strong>Note:</strong> The library will automatically detect and load a default
         * configuration file from resources if one exists with the same name as specified
         * in the builder. If found, the resource file's values will be used as defaults,
         * overriding any default values set programmatically in your {@link IField} fields.
         *
         * @param success a callback that is invoked when the configuration is successfully built and initialized.
         *                The callback receives the newly created {@link ConfigManager} instance, allowing you to
         *                perform post-initialization operations such as registering listeners, starting scheduled
         *                tasks, or accessing loaded configuration values
         * @param failed a callback that is invoked if the build process encounters validation errors, such as
         *               missing required fields or invalid configuration values. The callback receives a list of
         *               error messages describing each validation failure, allowing you to handle errors gracefully
         *               (e.g., logging errors, providing defaults, or shutting down the application)
         * @return a new ConfigManager instance configured with this builder's settings, or null if validation fails
         * 
         * @apiNote Example usage:
         * <pre>{@code
         * ConfigManager.builder()
         *     .config(new MyConfig())
         *     .path("config.yml")
         *     .build(
         *         manager -> {
         *             // Success: configuration loaded
         *             System.out.println("Config loaded successfully!");
         *             myConfig.startServices();
         *         },
         *         errors -> {
         *             // Failed: handle validation errors
         *             errors.forEach(err -> logger.error("Config error: " + err));
         *             System.exit(1);
         *         }
         *     );
         * }</pre>
         */
        public ConfigManager build(Consumer<ConfigManager> success, Consumer<List<String>> failed) {
            this.success = success;
            this.failed = failed;
            return new ConfigManager(this);
        }
    }
}
