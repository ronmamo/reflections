package org.reflections;

import javassist.bytecode.AnnotationsAttribute;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class TestIssue144 {

    static Reflections reflections;
    public static final FilterBuilder TestModelFilter = new FilterBuilder().include("org.reflections.TestIssue144\\$.*");


    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()));
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Colors.class)
    @interface Color {
        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Colors {
        Color[] value();
    }


    @Color(name = "foo")
    class SingleName {}

    @Color(name = "foo")
    @Color(name = "bar")
    class MultiName  {}



    class TestFieldAndMethod{

        @Color(name = "foo")
        public int SingleNameField;

        @Color(name = "foo")
        @Color(name = "bar")
        public int MultiNameField;

        @Color(name = "foo")
        public void SingleNameMethod(){
        }

        @Color(name = "foo")
        @Color(name = "bar")
        public void MultiNameMethod(){

        }
    }


    @Test
    public void TestRepeatableAnnotationType(){
        Set<Class<?>> expected = new HashSet<>();

        expected.add(SingleName.class);
        expected.add(MultiName.class);


        Set<Class<?>> found = reflections.getTypesAnnotatedWith(Color.class);

        assertEquals(expected, found);
    }

    @Test
    public void TestRepeatableAnnotationMethod(){

        Set<Method> expected = new HashSet<>();

        try {
            expected.add(TestFieldAndMethod.class.getDeclaredMethod("SingleNameMethod"));
            expected.add(TestFieldAndMethod.class.getDeclaredMethod("MultiNameMethod"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        Set<Method> found = reflections.getMethodsAnnotatedWith(Color.class);

        assertEquals(expected, found);
    }

    @Test
    public void TestRepeatableAnnotationField(){
        Set<Field> expected = new HashSet<>();

        try {
            expected.add(TestFieldAndMethod.class.getDeclaredField("SingleNameField"));
            expected.add(TestFieldAndMethod.class.getDeclaredField("MultiNameField"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        Set<Field> found = reflections.getFieldsAnnotatedWith(Color.class);

        assertEquals(expected, found);
    }
}
