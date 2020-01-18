package org.reflections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class AnotherTestModel {

    @CyclicAnnotation
    public @Retention(RUNTIME) @interface CyclicAnnotation {}

    @Target(ElementType.TYPE)
    @Retention(RUNTIME)
    @interface Meta {
        String value();
    }

    @Meta("a")
    @Retention(RUNTIME)
    @interface A {}

    @Meta("b")
    @Retention(RUNTIME)
    @interface B {}

    @A class A1 {}
    @B class B1 {}
    @A class A2 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnnotation {
        public String value();
    }

    @TestAnnotation("foo foo foo")
    public class ActualFunctionalityClass {
        @TestAnnotation("bar bar bar")
        class Thing {
        }
    }

    public @org.reflections.TestModel.AC2("grr...") class C2 extends org.reflections.TestModel.C1 {}

}
