package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.reflections.MoreTestsModel.*;
import static org.reflections.ReflectionUtils.Annotations;
import static org.reflections.ReflectionUtils.SuperTypes;
import static org.reflections.ReflectionUtilsTest.toStringSorted;
import static org.reflections.ReflectionsTest.equalTo;
import static org.reflections.scanners.Scanners.Resources;
import static org.reflections.scanners.Scanners.SubTypes;

public class MoreTests {

    @Test
    public void test_cyclic_annotation() {
        Reflections reflections = new Reflections(MoreTestsModel.class);
        assertThat(reflections.getTypesAnnotatedWith(CyclicAnnotation.class),
                equalTo(CyclicAnnotation.class));
    }

    @Test
    public void no_exception_when_configured_scanner_store_is_empty() {
        Reflections reflections = new Reflections(
            new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(TestModel.class))
                .setScanners());

        assertNull(reflections.getStore().get(SubTypes.index()));
        assertTrue(reflections.getSubTypesOf(TestModel.C1.class).isEmpty());
        assertTrue(reflections.get(SubTypes.of(TestModel.C1.class)).isEmpty());
        assertTrue(reflections.get(Resources.with(".*")).isEmpty());
    }

    @Test
    public void getAllAnnotated_returns_meta_annotations() {
        Reflections reflections = new Reflections(MoreTestsModel.class);
        for (Class<?> type: reflections.getTypesAnnotatedWith(Meta.class)) {
            Set<Annotation> allAnnotations = ReflectionUtils.get(Annotations.of(type));
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            assertTrue(collect.contains(Meta.class));
        }

        Meta meta = new Meta() {
            @Override public String value() { return "a"; }
            @Override public Class<? extends Annotation> annotationType() { return Meta.class; }
        };
        for (Class<?> type: reflections.getTypesAnnotatedWith(meta)) {
            Set<Annotation> allAnnotations = ReflectionUtils.get(Annotations.of(type));
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            assertTrue(collect.contains(Meta.class));
        }
    }

    @Test
    public void test_custom_url_class_loader() throws MalformedURLException {
        URL externalUrl = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        URLClassLoader externalClassLoader = new URLClassLoader(new URL[]{externalUrl}, Thread.currentThread().getContextClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forClass(TestModel.class))
                .addUrls(externalUrl)
                .addClassLoaders(externalClassLoader));

        assertEquals(toStringSorted(reflections.getSubTypesOf(TestModel.C1.class)),
                "[class another.project.AnotherTestModel$C2, " +
                        "class org.reflections.TestModel$C2, " +
                        "class org.reflections.TestModel$C3, " +
                        "class org.reflections.TestModel$C5]");
    }

    @Test
    public void test_reflection_utils_with_custom_loader() throws MalformedURLException, ClassNotFoundException {
        URL url = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());

        Class<?> aClass = Class.forName("another.project.AnotherTestModel$C2", true, classLoader);
        assertEquals(toStringSorted(ReflectionUtils.get(SuperTypes.of(aClass), t -> !Object.class.equals(t))),
                "[class org.reflections.TestModel$C1, " +
                        "interface org.reflections.TestModel$I1, " +
                        "interface org.reflections.TestModel$I2]");
    }

    @Test
    public void resources_scanner_filters_classes() {
        Reflections reflections = new Reflections(Scanners.Resources);
        Collection<String> resources = reflections.getResources(".*");
        assertTrue(resources.stream().noneMatch(res -> res.endsWith(".class")));
    }

    @Test
    public void test_repeatable() {
        Reflections ref = new Reflections(MoreTestsModel.class);
        Collection<Class<?>> clazzes = ref.getTypesAnnotatedWith(Name.class);
        assertTrue(clazzes.contains(SingleName.class));
        assertFalse(clazzes.contains(MultiName.class));

        clazzes = ref.getTypesAnnotatedWith(Names.class);
        assertFalse(clazzes.contains(SingleName.class));
        assertTrue(clazzes.contains(MultiName.class));
    }

    @Test
    public void test_method_param_names_not_local_vars() throws NoSuchMethodException {
        Reflections reflections = new Reflections(MoreTestsModel.class, new MethodParameterNamesScanner());

        Class<ParamNames> clazz = ParamNames.class;
        assertEquals(reflections.getMemberParameterNames(clazz.getConstructor(String.class)).toString(),
                "[param1]");
        assertEquals(reflections.getMemberParameterNames(clazz.getMethod("test", String.class, String.class)).toString(),
                "[testParam1, testParam2]");
        assertEquals(reflections.getMemberParameterNames(clazz.getMethod("test", String.class)).toString(),
                "[testParam]");
        assertEquals(reflections.getMemberParameterNames(clazz.getMethod("test2", String.class)).toString(),
                "[testParam]");

    }

    @Test
    public void test_scan_all_classes_from_package() throws MalformedURLException {
        URL url = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(url)
                .setScanners(new SubTypesScanner(false))
                .addClassLoaders(classLoader));
        Set<Class<?>> allClass = reflections.getSubTypesOf(Object.class);
        assertEquals(34, allClass.size());

        Set<String> classNames = allClass.stream()
                .map(classType -> {return classType.getSimpleName();})
                .collect(Collectors.toSet());
        Set<String> expectedClassNames = new LinkedHashSet<>(Arrays.asList("Meta", "A", "B",
                "AM1", "TestAnnotation", "I1", "AI1", "I2", "AI2", "I3", "TestModel", "C1",
                "AC1", "C2", "A1", "AC2", "C3", "A2", "AC3", "C4", "C5", "C6", "C7",
                "AnotherTestModel", "Thing", "AC1n", "Usage", "ActualFunctionalityClass",
                "AF1", "B1", "CyclicAnnotation", "MAI1"));
        assertTrue(classNames.containsAll(expectedClassNames));

    }
}
