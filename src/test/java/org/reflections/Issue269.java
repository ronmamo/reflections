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
@interface Meta {}

@Meta
@Retention(RUNTIME)
@interface AM {}

@Meta
@Retention(RUNTIME)
@interface BM {}

class A1 {
    @AM public void inA1() {}
}

class A2 {
    @AM public void inA2() {}
}

class B1 {
    @BM public void inB1() {}
}

public class Issue269 {
    @Test
    public void testIssue269() throws NoSuchMethodException {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackages("org.reflections")
                        .addScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner()));

        Set<Method> expected = new HashSet<>();
        expected.add(A1.class.getMethod("inA1"));
        expected.add(A2.class.getMethod("inA2"));
        expected.add(B1.class.getMethod("inB1"));
        Set<Method> metaMethods = reflections.getMethodsAnnotatedWith(Meta.class);
        assertEquals(expected, metaMethods);
    }
}