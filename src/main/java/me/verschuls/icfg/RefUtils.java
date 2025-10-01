package me.verschuls.icfg;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class RefUtils {

    public static<T> RefClass<T> get(T instance) {
        return new RefClass<>(instance);
    }

    public static<T> RefClass<T> fromClass(Class<T> tClass) {
        return new RefClass<>(tClass);
    }

    public static Object invokeMethod(Object instance, String name, Object... args) {
        Class<?> class_ = instance.getClass();
        try {
            Method method = class_.getDeclaredMethod(name);
            method.setAccessible(true);
            if (method.getParameterCount() != args.length) {
                System.err.println("Couldn't invoke method \""+name+"\" in class \""+class_.getName()+"\" Args required: \""+method.getParameterCount()+"\" Args given: "+args.length);
                return null;
            }
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Exception while invoking method");
            e.printStackTrace(System.console().writer());
            return null;
        } catch (NoSuchMethodException e) {
            System.err.println("Couldn't locate method \""+name+"\" in class \""+class_.getName()+"\"");
            e.printStackTrace(System.console().writer());
            return null;
        }
    }
}
