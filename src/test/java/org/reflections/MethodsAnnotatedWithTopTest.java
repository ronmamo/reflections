package org.reflections;

import org.junit.Test;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;

@Retention(RUNTIME)
@interface Top {}

@Top
@Retention(RUNTIME)
@interface Mid {}

@Mid
@Retention(RUNTIME)
@interface Down {}

class C {
    @Down public void inC() {}
}

public class MethodsAnnotatedWithTopTest {

    @Test
    public void testMethodsAnnotatedWithTopTest() throws NoSuchMethodException {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackages("org.reflections")
                        .addScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner()));

        Set<Method> expected = new HashSet<>();
        expected.add(C.class.getMethod("inC"));
        Set<Method> metaMethods = reflections.getMethodsAnnotatedWith(Top.class);
        assertEquals(expected, metaMethods);
    }
}
