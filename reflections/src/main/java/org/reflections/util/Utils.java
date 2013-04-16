package org.reflections.util;

import com.google.common.collect.Sets;
import org.reflections.ReflectionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.reflections.ReflectionUtils.forName;

/**
 * a garbage can of convenient methods
 */
public abstract class Utils {

    public static String repeat(String string, int times) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < times; i++) {
            sb.append(string);
        }

        return sb.toString();
    }

    /**
     * isEmpty compatible with Java 5
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isEmpty(Object[] objects) {
        return objects == null || objects.length == 0;
    }

    public static File prepareFile(String filename) {
        File file = new File(filename);
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        return file;
    }

    public static Member getMemberFromDescriptor(String descriptor, ClassLoader... classLoaders) throws ReflectionsException {
        int p0 = descriptor.indexOf('(');
        String methodKey = descriptor.substring(0, p0);
        String methodParameters = descriptor.substring(p0 + 1, descriptor.length() - 1);

        int p1 = methodKey.lastIndexOf('.');
        String className = methodKey.substring(methodKey.lastIndexOf(' ') + 1, p1);
        String methodName = methodKey.substring(p1 + 1);

        Class<?>[] parameterTypes = null;
        if (!isEmpty(methodParameters)) {
            String[] parameterNames = methodParameters.split(",");
            List<Class<?>> result = new ArrayList<Class<?>>(parameterNames.length);
            for (String name : parameterNames) {
                result.add(forName(name.trim()));
            }
            parameterTypes = result.toArray(new Class<?>[result.size()]);
        }

        Class<?> aClass = forName(className, classLoaders);
        try {
            if (isConstructor(descriptor)) {
                return aClass.getConstructor(parameterTypes);
            } else {
                return aClass.getDeclaredMethod(methodName, parameterTypes);
            }
        } catch (NoSuchMethodException e) {
            throw new ReflectionsException("Can't resolve method named " + methodName, e);
        }
    }

    public static Set<Method> getMethodsFromDescriptors(Collection<String> annotatedWith, ClassLoader... classLoaders) {
        Set<Method> result = Sets.newHashSet();
        for (String annotated : annotatedWith) {
            if (!isConstructor(annotated)) {
                Method member = (Method) getMemberFromDescriptor(annotated, classLoaders);
                if (member != null) result.add(member);
            }
        }
        return result;
    }

    public static Set<Constructor> getConstructorsFromDescriptors(Collection<String> annotatedWith, ClassLoader... classLoaders) {
        Set<Constructor> result = Sets.newHashSet();
        for (String annotated : annotatedWith) {
            if (isConstructor(annotated)) {
                Constructor member = (Constructor) getMemberFromDescriptor(annotated, classLoaders);
                if (member != null) result.add(member);
            }
        }
        return result;
    }

    public static boolean isConstructor(String fqn) {
        return fqn.contains("init>");
    }

    public static Field getFieldFromString(String field, ClassLoader... classLoaders) {
        //todo create field md
        String className = field.substring(0, field.lastIndexOf('.'));
        String fieldName = field.substring(field.lastIndexOf('.') + 1);

        try {
            return forName(className, classLoaders).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ReflectionsException("Can't resolve field named " + fieldName, e);
        }
    }

    public static void close(InputStream closeable) {
        try { if (closeable != null) closeable.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public static @Nullable Logger findLogger(Class<?> aClass) {
        try {
            Class.forName("org.slf4j.impl.StaticLoggerBinder");
            return LoggerFactory.getLogger(aClass);
        } catch (Throwable e) {
            return null;
        }
    }

    public static <T> Set<T> intersect(Collection<T> ts1, Collection<T> ts2) {
        Set<T> result = Sets.newHashSet();
        for (T t : ts1) if (ts2.contains(t)) result.add(t);
        return result;
    }
}
