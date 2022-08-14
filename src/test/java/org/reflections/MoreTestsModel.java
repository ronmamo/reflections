package org.reflections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class MoreTestsModel {

    @CyclicAnnotation
    @Retention(RUNTIME)
    public @interface CyclicAnnotation {}

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

    @Retention(RUNTIME)
    public @interface TestAnnotation {
        String value();
    }

    @TestAnnotation("foo foo foo")
    public class ActualFunctionalityClass {
        @TestAnnotation("bar bar bar")
        class Thing { }
    }

    // repeatable
    @Repeatable(Names.class)
    @Retention(RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Name {
        String name();
    }

    @Name(name = "foo")
    @Name(name = "bar")
    public static class MultiName { }

    @Retention(RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Names {
        Name[] value() default {};
    }

    @Name(name = "foo")
    public static class SingleName { }

    //
    public static class ParamNames {
        public ParamNames() {
            String testLocal = "local";
        }
        public ParamNames(String param1) {
            String testLocal = "local";
        }
        public void test(String testParam) {
            String testLocal = "local";
        }
        public void test(String testParam1, String testParam2) {
            String testLocal1 = "local";
            String testLocal2 = "local";
        }

        public static void test2(String testParam) {
            String testLocal = "local";
        }
    }
}
