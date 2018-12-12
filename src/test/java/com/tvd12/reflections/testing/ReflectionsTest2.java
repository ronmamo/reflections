package com.tvd12.reflections.testing;

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

import com.tvd12.reflections.Reflections;
import com.tvd12.reflections.scanners.SubTypesScanner;
import com.tvd12.reflections.testing.TestModel.C1;
import com.tvd12.reflections.testing.TestModel.C2;
import com.tvd12.reflections.testing.TestModel.C3;
import com.tvd12.reflections.testing.TestModel.C5;
import com.tvd12.reflections.testing.TestModel.I1;
import com.tvd12.reflections.testing.TestModel.I2;
import com.tvd12.reflections.util.ClasspathHelper;
import com.tvd12.reflections.util.ConfigurationBuilder;
import com.tvd12.reflections.util.FilterBuilder;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ReflectionsTest2 {
    public static final FilterBuilder TestModelFilter = new FilterBuilder().include("com.tvd12.reflections.testing.TestModel\\$.*");
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
