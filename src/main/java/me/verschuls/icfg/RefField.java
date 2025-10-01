package me.verschuls.icfg;

import me.verschuls.icfg.annotations.IField;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

class RefField<T> {

    private IField iField = null;
    private final Field field;
    private final RefClass<?> refClass;

    RefField(Field field_, RefClass<?> refClass) {
        this.field = field_;
        this.refClass = refClass;
        field.setAccessible(true);
        if (field.isAnnotationPresent(IField.class)) iField = field.getAnnotation(IField.class);
    }

    public IField getAnnotation() {
        return iField;
    }

    public boolean isAnnotated() {
        return iField != null;
    }

    public String getName() {
        field.setAccessible(true);
        return field.getName();
    }

    public Class<T> getType() {
        field.setAccessible(true);
        return (Class<T>) field.getType();
    }

    public Class<?> getListType() {
        if (List.class.isAssignableFrom(field.getType())) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            return (Class<?>) listType.getActualTypeArguments()[0];
        }
        return null;
    }

    public void set(T value) {
        try {
            field.set(refClass.getInstance(), value);
        } catch (IllegalAccessException e) {
            System.err.println("Exception while setting filed \""+getName()+"\"");
            e.printStackTrace(System.console().writer());
        }
    }

    public T get() {
        try {
            return (T) field.get(refClass.getInstance());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            System.err.println("Exception while getting filed \""+getName()+"\"");
            e.printStackTrace(System.console().writer());
            return null;
        }
    }
}
