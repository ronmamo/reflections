package org.reflections;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.TestModel.C1;
import org.reflections.TestModel.C2;
import org.reflections.TestModel.C3;
import org.reflections.TestModel.C5;
import org.reflections.TestModel.I1;
import org.reflections.TestModel.I2;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ReflectionsTest2 {
    public static final FilterBuilder TestModelFilter = new FilterBuilder().include("org.reflections.TestModel\\$.*");
    static Reflections reflections;

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false)//,
//                        new TypeAnnotationsScanner(),
//                        new FieldAnnotationsScanner(),
//                        new MethodAnnotationsScanner(),
//                        new MethodParameterScanner(),
//                        new MethodParameterNamesScanner(),
//                        new MemberUsageScanner()
                        ));
    }

    @Test
    public void testSubTypesOf() {
    		Set<Class<? extends I1>> subTypesOf = reflections.getSubTypesOf(I1.class);
        assertThat(subTypesOf, are(I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getSubTypesOf(C1.class), are(C2.class, C3.class, C5.class));

        assertFalse("getAllTypes should not be empty when Reflections is configured with SubTypesScanner(false)",
                reflections.getAllTypes().isEmpty());
    }

    private abstract static class Match<T> extends BaseMatcher<T> {
        public void describeTo(Description description) { }
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
}
