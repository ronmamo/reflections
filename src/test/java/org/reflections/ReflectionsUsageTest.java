package org.reflections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.reflections.ReflectionUtils.getAllConstructors;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withParametersCount;
import static org.reflections.ReflectionUtils.withPrefix;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.SubTypesScanner;

@SuppressWarnings("unchecked")
public class ReflectionsUsageTest {

    static Reflections reflections;

    @BeforeClass
    public static void init() throws MalformedURLException {
        URL externalUrl = new URL(
            "jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/usages-project-1.0.jar!/");
        URLClassLoader externalClassLoader = new URLClassLoader(new URL[]{externalUrl},
            Thread.currentThread().getContextClassLoader());

        String basePackage = "tests.usages";
        reflections = new Reflections(
            basePackage,
            new MemberUsageScanner(),
            new SubTypesScanner(false),
            externalClassLoader);
    }

    @Test
    public void test_find_field_usages_using_custom_url_class_loader() {
        try {
            Set<Member> members = reflections.getSubTypesOf(Object.class).stream()
                .filter(clz -> "tests.usages.model.Game".equals(clz.getName()))
                .map(this::retrieveFields)
                .flatMap(Collection::stream)
                .map(reflections::getFieldUsage)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
            assertThat(members.isEmpty(), is(false));
        } catch (ReflectionsException e) {
            fail();
        }
    }

    @Test
    public void test_find_method_usages_using_custom_url_class_loader() {
        try {
            Set<Member> members = reflections.getSubTypesOf(Object.class).stream()
                .filter(clz -> "tests.usages.model.Game".equals(clz.getName()))
                .map(this::retrieveGetters)
                .flatMap(Collection::stream)
                .map(reflections::getMethodUsage)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
            assertThat(members.isEmpty(), is(false));
        } catch (ReflectionsException e) {
            fail();
        }
    }

    @Test
    public void test_find_constructor_usages_using_custom_url_class_loader() {
        try {
            Set<Member> members = reflections.getSubTypesOf(Object.class).stream()
                .filter(clz -> "tests.usages.model.Game".equals(clz.getName()))
                .map(this::retrieveConstructors)
                .flatMap(Collection::stream)
                .map(reflections::getConstructorUsage)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
            assertThat(members.isEmpty(), is(false));
        } catch (ReflectionsException e) {
            fail();
        }
    }

    private Set<Field> retrieveFields(Class<?> clz) {
        return getAllFields(clz, withModifier(Modifier.PUBLIC));
    }

    private Set<Method> retrieveGetters(Class<?> clz) {
        return getAllMethods(clz,
            withModifier(Modifier.PUBLIC), withPrefix("get"), withParametersCount(0));
    }

    private Set<Constructor> retrieveConstructors(Class<?> clz) {
        return getAllConstructors(clz, withModifier(Modifier.PUBLIC));
    }
}
