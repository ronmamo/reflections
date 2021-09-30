package com.tvd12.reflections.testing;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tvd12.reflections.Reflections;
import com.tvd12.reflections.scanners.FieldAnnotationsScanner;
import com.tvd12.reflections.scanners.MemberUsageScanner;
import com.tvd12.reflections.scanners.MethodAnnotationsScanner;
import com.tvd12.reflections.scanners.MethodParameterNamesScanner;
import com.tvd12.reflections.scanners.MethodParameterScanner;
import com.tvd12.reflections.scanners.SubTypesScanner;
import com.tvd12.reflections.scanners.TypeAnnotationsScanner;
import com.tvd12.reflections.testing.TestModel.AF1;
import com.tvd12.reflections.testing.TestModel.C4;
import com.tvd12.reflections.util.ClasspathHelper;
import com.tvd12.reflections.util.ConfigurationBuilder;
import com.tvd12.reflections.util.FilterBuilder;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ReflectionsTest3 {
    public static final FilterBuilder TestModelFilter = new FilterBuilder().include("com.tvd12.reflections.testing.TestModel\\$.*");
    static Reflections reflections;

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class)))
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

    @Test
    public void testFieldsAnnotatedWith() {
        try {
            assertThat(reflections.getFieldsAnnotatedWith(AF1.class),
                    are(C4.class.getDeclaredField("f1"),
                        C4.class.getDeclaredField("f2")
                        ));

            assertThat(reflections.getFieldsAnnotatedWith(new AF1() {
                            public String value() {return "2";}
                            public Class<? extends Annotation> annotationType() {return AF1.class;}}),
                    are(C4.class.getDeclaredField("f2")));
        } catch (NoSuchFieldException e) {
            fail();
        }
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
