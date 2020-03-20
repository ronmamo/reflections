package org.reflections.adapters;

import javassist.ClassPool;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import org.reflections.ReflectionsException;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.Utils;
import org.reflections.vfs.Vfs;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static javassist.bytecode.AccessFlag.isPrivate;
import static javassist.bytecode.AccessFlag.isProtected;
import static org.reflections.util.Utils.join;

/**
 *
 */
public class JavassistAdapter implements MetadataAdapter<ClassFile, FieldInfo, MethodInfo> {

    private ClassLoader[] classLoaders;

    public JavassistAdapter() {
        this.classLoaders = null;
    }

    public JavassistAdapter(ClassLoader[] classLoaders) {
        this.classLoaders = classLoaders;
    }

    /**setting this to false will result in returning only visible annotations from the relevant methods here (only {@link java.lang.annotation.RetentionPolicy#RUNTIME})*/
    public static boolean includeInvisibleTag = true;

    public List<FieldInfo> getFields(final ClassFile cls) {
        return cls.getFields();
    }

    public List<MethodInfo> getMethods(final ClassFile cls) {
        return cls.getMethods();
    }

    public String getMethodName(final MethodInfo method) {
        return method.getName();
    }

    public List<String> getParameterNames(final MethodInfo method) {
        String descriptor = method.getDescriptor();
        descriptor = descriptor.substring(descriptor.indexOf("(") + 1, descriptor.lastIndexOf(")"));
        return splitDescriptorToTypeNames(descriptor);
    }

    public List<String> getClassAnnotationNames(final ClassFile aClass) {
        return getAnnotationNames((AnnotationsAttribute) aClass.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) aClass.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    public List<String> getFieldAnnotationNames(final FieldInfo field) {
        return getAnnotationNames((AnnotationsAttribute) field.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) field.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    public List<String> getMethodAnnotationNames(final MethodInfo method) {
        return getAnnotationNames((AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    public List<String> getParameterAnnotationNames(final MethodInfo method, final int parameterIndex) {
        List<String> result = new ArrayList<>();

        List<ParameterAnnotationsAttribute> parameterAnnotationsAttributes = Arrays.asList(
                (ParameterAnnotationsAttribute) method.getAttribute(ParameterAnnotationsAttribute.visibleTag),
                (ParameterAnnotationsAttribute) method.getAttribute(ParameterAnnotationsAttribute.invisibleTag));

        for (ParameterAnnotationsAttribute parameterAnnotationsAttribute : parameterAnnotationsAttributes) {
            if (parameterAnnotationsAttribute != null) {
                Annotation[][] annotations = parameterAnnotationsAttribute.getAnnotations();
                if (parameterIndex < annotations.length) {
                    Annotation[] annotation = annotations[parameterIndex];
                    result.addAll(getAnnotationNames(annotation));
                }
            }
        }

        return result;
    }

    public String getReturnTypeName(final MethodInfo method) {
        String descriptor = method.getDescriptor();
        descriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
        return splitDescriptorToTypeNames(descriptor).get(0);
    }

    public String getFieldName(final FieldInfo field) {
        return field.getName();
    }

    public ClassFile getOrCreateClassObject(final Vfs.File file) {
        InputStream inputStream = null;
        try {
            inputStream = file.openInputStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(inputStream));
            return new ClassFile(dis);
        } catch (IOException e) {
            throw new ReflectionsException("could not create class file from " + file.getName(), e);
        } finally {
            Utils.close(inputStream);
        }
    }

    public String getMethodModifier(MethodInfo method) {
        int accessFlags = method.getAccessFlags();
        return isPrivate(accessFlags) ? "private" :
               isProtected(accessFlags) ? "protected" :
               isPublic(accessFlags) ? "public" : "";
    }

    public String getMethodKey(ClassFile cls, MethodInfo method) {
        return getMethodName(method) + "(" + join(getParameterNames(method), ", ") + ")";
    }

    public String getMethodFullKey(ClassFile cls, MethodInfo method) {
        return getClassName(cls) + "." + getMethodKey(cls, method);
    }

    public boolean isPublic(Object o) {
        Integer accessFlags =
                o instanceof ClassFile ? ((ClassFile) o).getAccessFlags() :
                o instanceof FieldInfo ? ((FieldInfo) o).getAccessFlags() :
                o instanceof MethodInfo ? ((MethodInfo) o).getAccessFlags() : null;

        return accessFlags != null && AccessFlag.isPublic(accessFlags);
    }

    //
    public String getClassName(final ClassFile cls) {
        return cls.getName();
    }

    public String getSuperclassName(final ClassFile cls) {
        return cls.getSuperclass();
    }

    public List<String> getInterfacesNames(final ClassFile cls) {
        return Arrays.asList(cls.getInterfaces());
    }

    public boolean acceptsInput(String file) {
        return file.endsWith(".class");
    }
    
    //
    private List<String> getAnnotationNames(final AnnotationsAttribute... annotationsAttributes) {
        if (annotationsAttributes != null) {
            return Arrays.stream(annotationsAttributes)
                    .filter(Objects::nonNull)
                    .flatMap(annotationsAttribute -> Arrays.stream(annotationsAttribute.getAnnotations()))
                    .map(Annotation::getTypeName)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> getAnnotationNames(final Annotation[] annotations) {
        return Arrays.stream(annotations).map(Annotation::getTypeName).collect(Collectors.toList());
    }

    private List<String> splitDescriptorToTypeNames(final String descriptors) {
        List<String> result = new ArrayList<>();

        if (descriptors != null && descriptors.length() != 0) {

            List<Integer> indices = new ArrayList<>();
            Descriptor.Iterator iterator = new Descriptor.Iterator(descriptors);
            while (iterator.hasNext()) {
                indices.add(iterator.next());
            }
            indices.add(descriptors.length());

            result = IntStream.range(0, indices.size() - 1)
                    .mapToObj(i -> Descriptor.toString(descriptors.substring(indices.get(i), indices.get(i + 1))))
                    .collect(Collectors.toList());

        }

        return result;
    }

    @Override
    public List<String> getMetaMethodAnnotationNames(MethodInfo method) {
        return getAnnotationNamesRecursive((AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    @Override
    public List<String> getMetaFieldAnnotationNames(FieldInfo field) {
        return getAnnotationNamesRecursive((AnnotationsAttribute) field.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) field.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    public List<String> getAnnotationNamesRecursive(final AnnotationsAttribute... annotationsAttributes) {
        return Arrays.stream(annotationsAttributes)
                .filter(Objects::nonNull)
                .flatMap(annotationsAttribute -> getMetaAnnotations(new HashSet<>(), annotationsAttributes).stream())
                .map(Annotation::getTypeName)
                .collect(Collectors.toList());
    }

    private Set<Annotation> getMetaAnnotations(Set<String> mappedAnnotations, final AnnotationsAttribute... annotationsAttribute) {
        Set<String> alreadyMapped = new HashSet<>(mappedAnnotations);
        List<Annotation> annotations = Arrays.stream(annotationsAttribute)
                .filter(Objects::nonNull)
                .flatMap(it -> Arrays.stream(it.getAnnotations()))
                .collect(Collectors.toList());

        alreadyMapped.addAll(
                annotations.stream()
                    .map(Annotation::getTypeName)
                    .collect(Collectors.toList())
        );

        List<Annotation> children = annotations.stream()
                .filter(it -> !mappedAnnotations.contains(it.getTypeName()))
                .flatMap((it) -> {
                    try {
                        ClassFile annotationClassFile = getClassPool().getCtClass(it.getTypeName()).getClassFile();
                        return getMetaAnnotations(alreadyMapped, (AnnotationsAttribute) annotationClassFile.getAttribute(AnnotationsAttribute.visibleTag), includeInvisibleTag ? (AnnotationsAttribute) annotationClassFile.getAttribute(AnnotationsAttribute.invisibleTag) : null).stream();
                    } catch (NotFoundException e) {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        Set<Annotation> result = new HashSet<>();
        result.addAll(annotations);
        result.addAll(children);

        return result;
    }

    private ClassPool classPool;

    private ClassPool getClassPool() {
        if (classPool == null) {
            synchronized (this) {
                classPool = new ClassPool();
                ClassLoader[] classLoaders = this.classLoaders;
                if (classLoaders == null) {
                    classLoaders = ClasspathHelper.classLoaders();
                }
                for (ClassLoader classLoader : classLoaders) {
                    classPool.appendClassPath(new LoaderClassPath(classLoader));
                }
            }
        }
        return classPool;
    }
}
