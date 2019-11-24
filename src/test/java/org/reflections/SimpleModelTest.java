package org.reflections;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.reflections.ReflectionsTest.TestModelFilter;

public class SimpleModelTest {


    private static Reflections reflections;

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(asList(ClasspathHelper.forClass(SimpleModel.class)))
//                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()));
    }

    @Test
    public void test() {
        assertThat(reflections.getSubTypesOf(SimpleModel.SimpleSuperInterface.class),
                are(SimpleModel.SimpleChildInterface.class));
    }

    public static <T> Matcher<Set<? super T>> are(final T... ts) {
        final Collection<?> c1 = Arrays.asList(ts);
        return new Match<Set<? super T>>() {
            public boolean matches(Object o) {
                Collection<?> c2 = (Collection<?>) o;
                return c1.containsAll(c2) && c2.containsAll(c1);
            }
        };
    }

    private abstract static class Match<T> extends BaseMatcher<T> {
        public void describeTo(Description description) { }
    }
}
