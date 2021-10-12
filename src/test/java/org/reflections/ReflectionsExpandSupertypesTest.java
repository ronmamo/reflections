package org.reflections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import test.classes.a.BaseClass;
import test.classes.a.TestAnnotation;
import test.classes.b.ChildrenClass;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReflectionsExpandSupertypesTest {

    private final FilterBuilder inputsFilter = new FilterBuilder()
        .includePattern("org\\.reflections\\.ReflectionsExpandSupertypesTest\\$TestModel\\$ScannedScope\\$.*");

    @SuppressWarnings("unused")
    public interface TestModel {
        interface A { } // outside of scanned scope
        interface B extends A { } // outside of scanned scope, but immediate supertype

        interface ScannedScope {
            interface C extends B { }
            interface D extends B { }
        }
    }

    @Test
    public void testExpandSupertypes() {
        Reflections refExpand = new Reflections(new ConfigurationBuilder().
                setUrls(ClasspathHelper.forClass(TestModel.ScannedScope.C.class)).
                filterInputsBy(inputsFilter));
        assertTrue(refExpand.getConfiguration().shouldExpandSuperTypes());
        Collection<Class<? extends TestModel.A>> subTypesOf = refExpand.getSubTypesOf(TestModel.A.class);
        assertTrue(subTypesOf.contains(TestModel.B.class), "expanded");
        assertTrue(subTypesOf.containsAll(refExpand.getSubTypesOf(TestModel.B.class)), "transitivity");
    }

    @Test
    public void testNotExpandSupertypes() {
        Reflections refDontExpand = new Reflections(
            new ConfigurationBuilder()
                .forPackage("org.reflections")
                .filterInputsBy(inputsFilter).
                setExpandSuperTypes(false));
        assertFalse(refDontExpand.getConfiguration().shouldExpandSuperTypes());
        Collection<Class<? extends TestModel.A>> subTypesOf1 = refDontExpand.getSubTypesOf(TestModel.A.class);
        assertFalse(subTypesOf1.contains(TestModel.B.class));
    }

    @Test
    void testDetectInheritedAnnotations() {
        final Reflections reflections = new Reflections("test.classes.b");
        final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(TestAnnotation.class);

        final Set<Class<? extends BaseClass>> expected =
                Stream.of(BaseClass.class, ChildrenClass.class).collect(Collectors.toSet());
        Assertions.assertEquals(typesAnnotatedWith, expected);
    }
}
