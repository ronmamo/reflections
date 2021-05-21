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
@interface MetaBase {}

@MetaBase
@Retention(RUNTIME)
@interface AM {}

@MetaBase
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

public class MethodsAnnotatedWithMetaBaseTest {

    @Test
    public void testMetaAnnotationsFromBaseClassMethods() throws NoSuchMethodException {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackages("org.reflections")
                        .addScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner()));

        Set<Method> expected = new HashSet<>();
        expected.add(A1.class.getMethod("inA1"));
        expected.add(A2.class.getMethod("inA2"));
        expected.add(B1.class.getMethod("inB1"));
        Set<Method> metaMethods = reflections.getMethodsAnnotatedWith(MetaBase.class);
        assertEquals(expected, metaMethods);
    }
}
