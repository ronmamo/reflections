package org.reflections.adapters;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.reflections.util.Utils;
import org.reflections.vfs.Vfs;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.reflections.ReflectionUtils.forName;

/** */
public class JavaReflectionAdapter implements MetadataAdapter<Class, Field, Member> {

    public List<Field> getFields(Class cls) {
        return Lists.newArrayList(cls.getDeclaredFields());
    }

    public List<Member> getMethods(Class cls) {
        List<Member> methods = Lists.newArrayList();
        methods.addAll(Arrays.asList(cls.getDeclaredMethods()));
        methods.addAll(Arrays.asList(cls.getDeclaredConstructors()));
        return methods;
    }

    public String getMethodName(Member method) {
        return method instanceof Method ? method.getName() :
                method instanceof Constructor ? "<init>" : null;
    }

    public List<String> getParameterNames(final Member member) {
        List<String> result = Lists.newArrayList();

        Class<?>[] parameterTypes = member instanceof Method ? ((Method) member).getParameterTypes() :
                member instanceof Constructor ? ((Constructor) member).getParameterTypes() : null;

        if (parameterTypes != null) {
            for (Class<?> paramType : parameterTypes) {
                String name = getName(paramType);
                result.add(name);
            }
        }

        return result;
    }

    public List<String> getClassAnnotationNames(Class aClass) {
        return getAnnotationNames(aClass.getDeclaredAnnotations());
    }

    public List<String> getFieldAnnotationNames(Field field) {
        return getAnnotationNames(field.getDeclaredAnnotations());
    }

    public List<String> getMethodAnnotationNames(Member method) {
        Annotation[] annotations =
                method instanceof Method ? ((Method) method).getDeclaredAnnotations() :
                method instanceof Constructor ? ((Constructor) method).getDeclaredAnnotations() : null;
        return getAnnotationNames(annotations);
    }

    public List<String> getParameterAnnotationNames(Member method, int parameterIndex) {
        Annotation[][] annotations =
                method instanceof Method ? ((Method) method).getParameterAnnotations() :
                method instanceof Constructor ? ((Constructor) method).getParameterAnnotations() : null;

        return getAnnotationNames(annotations != null ? annotations[parameterIndex] : null);
    }

    public String getReturnTypeName(Member method) {
        return ((Method) method).getReturnType().getName();
    }

    public String getFieldName(Field field) {
        return field.getName();
    }

    public Class getOfCreateClassObject(Vfs.File file) throws Exception {
        return getOfCreateClassObject(file, null);
    }

    public Class getOfCreateClassObject(Vfs.File file, @Nullable ClassLoader... loaders) throws Exception {
        String name = file.getRelativePath().replace("/", ".").replace(".class", "");
        return forName(name, loaders);
    }

    public String getMethodModifier(Member method) {
        return Modifier.toString(method.getModifiers());
    }

    public String getMethodKey(Class cls, Member method) {
        return getMethodName(method) + "(" + Joiner.on(", ").join(getParameterNames(method)) + ")";
    }

    public String getMethodFullKey(Class cls, Member method) {
        return getClassName(cls) + "." + getMethodKey(cls, method);
    }

    public boolean isPublic(Object o) {
        Integer mod =
                o instanceof Class ? ((Class) o).getModifiers() :
                o instanceof Member ? ((Member) o).getModifiers() : null;

        return mod != null && Modifier.isPublic(mod);
    }

    public String getClassName(Class cls) {
        return cls.getName();
    }

    public String getSuperclassName(Class cls) {
        Class superclass = cls.getSuperclass();
        return superclass != null ? superclass.getName() : "";
    }

    public List<String> getInterfacesNames(Class cls) {
        Class[] classes = cls.getInterfaces();
        List<String> names = new ArrayList<String>(classes != null ? classes.length : 0);
        if (classes != null) for (Class cls1 : classes) names.add(cls1.getName());
        return names;
    }

    public boolean acceptsInput(String file) {
        return file.endsWith(".class");
    }
    
    //
    private List<String> getAnnotationNames(Annotation[] annotations) {
        List<String> names = new ArrayList<String>(annotations.length);
        for (Annotation annotation : annotations) {
            names.add(annotation.annotationType().getName());
        }
        return names;
    }

    public static String getName(Class type) {
        if (type.isArray()) {
            try {
                Class cl = type;
                int dim = 0; while (cl.isArray()) { dim++; cl = cl.getComponentType(); }
                return cl.getName() + Utils.repeat("[]", dim);
            } catch (Throwable e) {
                //
            }
        }
        return type.getName();
    }
}
