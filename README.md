# IsekaiConfig

A lightweight, reflection-based YAML configuration library for Java 21, designed to simplify class-based config management by automatically injecting values from YAML files into annotated or matching fields. No more repetitive `config.get...` calls—let reflection handle the mapping for faster, cleaner development. Built on top of BoostedYAML (with a planned switch to SnakeYAML for better performance). Originated from a private Discord bot's config workflow, now evolved for broader use. Future plans include JSON support, dependency injection, and more optimizations to keep it fast and simple.

Ideal for general Java applications, but particularly suited for Minecraft plugin/mod developers (e.g., Spigot/Bukkit) and JDA (Java Discord API) bot creators who need quick, type-safe configs without boilerplate.

## Features
- **Automatic Field Injection**: Maps YAML values to class fields via reflection, either by exact field name matching or custom paths using `@IField(path = "custom.path", required = true/false)`.
- **Required Fields Handling**: If a required field (via annotation or config settings) is missing, it logs errors and invokes a consumer with a list of missing paths.
- **Nested Configurations**: Fields can implement `IConfig` for hierarchical, nested YAML structures.
- **Config Class Metadata**: Use `@ConfigInfo(name = "root.path", required = true/false, allFields = true/false)` to define root paths, enforce requirements across all fields, or enable annotation-free matching (ignores `@IField` and requires exact name matches).
- **Post-Injection Hooks**: Annotate methods with `@OnInject` to run custom actions right after injection completes.
- **Supported Types**: Handles primitives (String, int, double, float), Lists, Sections (YAML subsections), HashMaps for quick conversions without extra classes, and custom instances (limited to single-argument constructors for now).
- **Config Manager**: Central builder for loading/reloading YAML files, registering configs, handling resource creation (from JAR or file), versioning, debugging, and callbacks for success/errors/reloads.
- **Reload Support**: Synchronous reloads with consumer hooks for post-reload actions.
- **Lightweight Focus**: Minimal dependencies, aimed at efficiency for real-world apps like bots and plugins.

## Installation
The library will be available via a hosted repository (details coming soon—check the repo for updates). For now, clone the repo and build locally, or add as a dependency once published.

**Maven (placeholder...):**
```xml
<dependency>
    <groupId>me.verschuls.icfg</groupId>
    <artifactId>isekaiconfig</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**Gradle (placeholder):**
```groovy
implementation 'me.verschuls.icfg:isekaiconfig:1.0-SNAPSHOT'
```

## TODO
- [ ] Add proper versioning
- [ ] No resource creator improvement
- [ ] Add caching for faster reload
- [ ] Remove IConfig for less code
- [ ] Make builder more universal
- [ ] Switch to SnakeYAML
- [ ] Ability to add comments
- [ ] General optimization
- [ ] Making lightweight
- [ ] Implement Dependency Injection (DI)

## Contributing
Pull requests are welcome! Fork the repo, make changes, and submit. Focus on the TODO items for quick wins.
