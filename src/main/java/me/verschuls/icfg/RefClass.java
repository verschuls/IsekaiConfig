package me.verschuls.icfg;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class RefClass<T> {

    private final Class<T> class_;

    private Constructor<T> constructor;

    private T instance;

    private final List<RefField> fields = new ArrayList<>();

    RefClass(Class<T> class_) {
        this.class_ = class_;
        for (Constructor cs : class_.getDeclaredConstructors()) {
            cs.setAccessible(true);
            if (cs.getParameterCount() > 1) continue;
            this.constructor = cs;
            break;
        }
        for (Field field : class_.getDeclaredFields()) {
            field.setAccessible(true);
            if (List.class.isAssignableFrom(field.getType())) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> type = (Class<?>)listType.getActualTypeArguments()[0];
                if (!ConfigManager.ALLOWED_IN_LIST.contains(type)) continue;
            }
            if (!isAllowed(field.getType())) continue;
            fields.add(new RefField<>(field, this));
        }
    }


    private static boolean isAllowed(Class<?> fieldType) {
        if (ConfigManager.ALLOWED_BASIC.contains(fieldType)) return true;
        if (fieldType.equals(HashMap.class)) return true;
        if (fieldType.equals(IConfig.class)) return true;
        if (fieldType.getDeclaredConstructors().length == 0) return false;
        return fieldType.getDeclaredConstructors()[0].getParameterCount() <= 1;
    }

    RefClass(T instance) {
        this.class_ = (Class<T>) instance.getClass();
        this.instance = instance;
        for (Field field : class_.getDeclaredFields()) {
            field.setAccessible(true);
            if (!isAllowed(field.getType())) continue;
            fields.add(new RefField<>(field, this));
        }
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotation) {
        return class_.getAnnotation(annotation);
    }

    public boolean isAnnotated(Class<? extends Annotation> annotation) {
        return getAnnotation(annotation) != null;
    }


    public boolean isAssignableFrom(Class<?> class_) {
        return class_.isAssignableFrom(class_);
    }

    public T createInstance() {
        try {
            return this.instance == null ? this.instance = constructor.newInstance() : this.instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return null;
        }
    }


    public T createInstance(Object... args) {
        try {
            return this.instance == null ? this.instance = constructor.newInstance(args) : this.instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    public T getInstance() {
        return instance;
    }

    public boolean hasConstructor() {
        return constructor != null;
    }

    public boolean hasArguments() {
        return constructor.getParameterCount() > 1;
    }

    public Class<?>[] getArgTypes() {
        return constructor.getParameterTypes();
    }

    public List<RefField> getFields() {
        return fields;
    }

    public List<RefField> getAnnotatedFields() {
        return getFields().stream().filter(RefField::isAnnotated).toList();
    }

}
