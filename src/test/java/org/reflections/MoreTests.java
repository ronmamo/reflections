package org.reflections;

import org.junit.Assert;
import org.junit.Test;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.reflections.AnotherTestModel.Meta;
import static org.reflections.AnotherTestModel.TestAnnotation;
import static org.reflections.ReflectionUtilsTest.toStringSorted;
import static org.reflections.ReflectionsTest.are;

public class MoreTests {

    @Test
    public void test_cyclic_annotation() {
        Reflections reflections = new Reflections(AnotherTestModel.class);
        assertThat(reflections.getTypesAnnotatedWith(AnotherTestModel.CyclicAnnotation.class),
                are(AnotherTestModel.CyclicAnnotation.class));
    }

    @Test
    public void no_exception_when_configured_scanner_store_is_empty() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder().includePackage("my.project.prefix")));

        reflections.getSubTypesOf(String.class);
    }

    @Test
    public void getAllAnnotated_returns_meta_annotations() {
        Reflections reflections = new Reflections(AnotherTestModel.class);
        for (Class<?> type: reflections.getTypesAnnotatedWith(Meta.class)) {
            Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            Assert.assertTrue(collect.contains(Meta.class));
        }

        Meta meta = new Meta() {
            @Override public String value() { return "a"; }
            @Override public Class<? extends Annotation> annotationType() { return Meta.class; }
        };
        for (Class<?> type: reflections.getTypesAnnotatedWith(meta)) {
            Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            Assert.assertTrue(collect.contains(Meta.class));
        }
    }

    @Test
    public void external_jar_inner_class_annotation() throws MalformedURLException {
        Reflections reflections = new Reflections(AnotherTestModel.class);
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(TestAnnotation.class);
        Assert.assertEquals(toStringSorted(typesAnnotatedWith),
                "[class org.reflections.AnotherTestModel$ActualFunctionalityClass, " +
                        "class org.reflections.AnotherTestModel$ActualFunctionalityClass$Thing]");

        URL url = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        Reflections reflections1 = new Reflections(url);
        Store store = reflections1.getStore();

        assertEquals(toStringSorted(store.get(TypeAnnotationsScanner.class, "another.project.AnotherTestModel$TestAnnotation")),
                "[another.project.AnotherTestModel$ActualFunctionalityClass, " +
                        "another.project.AnotherTestModel$ActualFunctionalityClass$Thing]");
    }

    @Test
    public void test_java_9_subtypes_of_Object() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(Object.class))
                .setScanners(new SubTypesScanner(false)));
        Set<?> components = reflections.getSubTypesOf(Object.class);
        assertFalse(components.isEmpty());
    }

    @Test
    public void test_custom_url_class_loader() throws MalformedURLException {
        URL url = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());

        Reflections reflections = new Reflections(url, classLoader);
        Store store = reflections.getStore();
        assertEquals(toStringSorted(store.get(TypeAnnotationsScanner.class, "another.project.TestModel$AC1")),
                "[another.project.TestModel$C1]");
    }

    @Test
    public void test_expand_supertypes_from_another_jar() throws MalformedURLException {
        URL url = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        Reflections reflections = new Reflections(url);

        assertEquals(toStringSorted(reflections.getSubTypesOf(TestModel.C1.class)),
                "[class another.project.AnotherTestModel$C2]");

        Reflections reflections1 = new Reflections(url, TestModel.class);
        assertEquals(toStringSorted(reflections1.getSubTypesOf(TestModel.C1.class)),
                "[class org.reflections.AnotherTestModel$C2, " +
                        "class org.reflections.TestModel$C2, " +
                        "class org.reflections.TestModel$C3, " +
                        "class org.reflections.TestModel$C5]");
    }

    @Test
    public void test_reflection_utils_with_custom_loader() throws MalformedURLException, ClassNotFoundException {
        URL url = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());

        Class<?> aClass = Class.forName("another.project.AnotherTestModel$C2", true, classLoader);
        assertEquals(toStringSorted(ReflectionUtils.getAllSuperTypes(aClass)),
                "[class another.project.AnotherTestModel$C2, " +
                        "class org.reflections.TestModel$C1, " +
                        "interface org.reflections.TestModel$I1, " +
                        "interface org.reflections.TestModel$I2]");
    }

    @Test
    public void resources_scanner_filters_classes() {
        Reflections reflections = new Reflections(new ResourcesScanner());
        Set<String> keys = reflections.getStore().keys(ResourcesScanner.class.getSimpleName());
        assertTrue(keys.stream().noneMatch(res -> res.endsWith(".class")));
    }
}
