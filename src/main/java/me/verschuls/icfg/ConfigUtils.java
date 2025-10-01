package me.verschuls.icfg;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

class ConfigUtils {

    private final YamlDocument config;

    private ConfigUtils(YamlDocument config) {
        this.config = config;
    }

    protected static ConfigUtils of(YamlDocument cfg) {
        return new ConfigUtils(cfg);
    }

    private final List<String> errors = new ArrayList<>();

    public void addErrors(List<String> errors) {
        this.errors.addAll(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void reportError(String error) {
        errors.add(error);
    }

    public final List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    private <T> T getValueValidate(String path, Class<T> class_) {
        T defaultValue = (T) defaultValue(class_);
        if (class_.equals(String.class))
            defaultValue = null;
        T obj = getValue(path, class_);
        if (Objects.nonNull(obj)) return obj;
        errors.add(path);
        return defaultValue;
    }

    public <T> T getValue(String path, Class<T> class_) {
        try {
            Method method = config.getClass().getMethod("get"+ capitalizeWords(class_.getSimpleName().toLowerCase().replace("integer", "int")), String.class);
            return (T) method.invoke(config, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void configure(String path, RefField field, boolean required, boolean resources) {
        if (resources) {
            if (field.getType() == Section.class) {
                Section section = getSection(path);
                if (section == null) {
                    reportError(path);
                    return;
                }
                if (section.isEmpty(false) && required) {
                    reportError(path);
                    return;
                }
                field.set(section);
                return;
            }
            if (field.getType() == HashMap.class) {
                HashMap<String, Object> section = getSectionAsHashMap(path);
                if (section.isEmpty() && required) {
                    reportError(path);
                    return;
                }
                field.set(section);
                return;
            }
            field.set(required ? getValueValidate(path, field.getType()) : getValue(path, field.getType()));
            return;
        }
        if (required)
            if (field.get() == null) {
                reportError(path);
                return;
            }
        if (field.getType() == Section.class) {
            //if (LOGGER != null) LOGGER.accept("Skipping \""+path+"\" since its Section and has no defaults");
            field.set(null);
            return;
        }
        if (field.getType() == HashMap.class) {
            HashMap<String, Object> hashMap = ((HashMap<String, Object>)field.get());
            Section section = config.getSection(path);
            if (!section.isEmpty(false)) {
                for (String key : section.getKeys().stream().map(Object::toString).toList())
                    hashMap.put(key, section.get(key));
                return;
            }
            if (hashMap.isEmpty()) return;
            hashMap.forEach((k, v)-> {
                config.set(path+"."+k, v);
            });
            return;
        }
        Object value = getValue(path, field.getType());
        if (value == null) {
            field.set(getValue(path, field.getType()));
            return;
        }
        field.set(required ? getValueValidate(path, field.getType()) : getValue(path, field.getType()));
    }

    public boolean isEmpty(String path) {
        return config.get(path) == null;
    }

    public Section getSection(String route) {
        return config.getSection(route);
    }

    private  <K, V> HashMap<K, V> getSectionAsHashMap(String route) {
        HashMap<K, V> hashMap = new HashMap<>();
        Section section = config.getSection(route);
        for (Object key : section.getKeys())
            hashMap.put((K) key, (V) section.get((String) key));
        return hashMap;
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    private static String capitalizeWords(String input) {
        StringBuilder sb = new StringBuilder();
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                String lowercaseWord = word.toLowerCase();
                String capitalizedWord = lowercaseWord.substring(0, 1).toUpperCase() + lowercaseWord.substring(1);
                sb.append(capitalizedWord).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static Object defaultValue(Class<?> class_) {
        if (class_.equals(String.class)) return null;
        else if (class_.equals(Integer.class)) return 0;
        else if (class_.equals(Long.class)) return 0L;
        else if (class_.equals(Double.class)) return 0D;
        else if (class_.equals(Float.class)) return 0F;
        else if (class_.equals(Section.class)) return new HashMap<>();
        else if (class_.equals(List.class)) return new ArrayList<>();
        else return null;
    }

    @Deprecated
    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
