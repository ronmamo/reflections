package org.reflections;

import java.lang.annotation.*;
import org.junit.*;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

import org.reflections.scanners.*;
import org.reflections.util.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.*;

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

public class Issue269LongWay {
    @Test
    public void testIssue269LongWay() throws NoSuchMethodException {
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