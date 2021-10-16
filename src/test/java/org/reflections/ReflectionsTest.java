package org.reflections;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.Scanners;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.util.NameHelper;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.reflections.TestModel.*;

@SuppressWarnings("unchecked")
public class ReflectionsTest implements NameHelper {
    private static final FilterBuilder TestModelFilter = new FilterBuilder()
        .includePattern("org\\.reflections\\.TestModel\\$.*")
        .includePattern("org\\.reflections\\.UsageTestModel\\$.*");

    static Reflections reflections;

    @BeforeAll
    public static void init() {
        //noinspection deprecation
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                    new SubTypesScanner(),
                    new TypeAnnotationsScanner(),
                    new MethodAnnotationsScanner(),
                    new FieldAnnotationsScanner(),
                    Scanners.ConstructorsAnnotated,
                    Scanners.MethodsParameter,
                    Scanners.MethodsSignature,
                    Scanners.MethodsReturn,
                    Scanners.ConstructorsParameter,
                    Scanners.ConstructorsSignature,
                    new ResourcesScanner(),
                    new MethodParameterNamesScanner(),
                    new MemberUsageScanner()));
    }

    @Test
    public void testSubTypesOf() {
        assertThat(reflections.getSubTypesOf(I1.class), are(I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getSubTypesOf(C1.class), are(C2.class, C3.class, C5.class));

        assertFalse(reflections.getAllTypes().isEmpty(), "getAllTypes should not be empty when Reflections is configured with SubTypesScanner(false)");
    }

    @Test
    public void testTypesAnnotatedWith() {
        assertThat(reflections.getTypesAnnotatedWith(MAI1.class, true), are(AI1.class));
        assertThat(reflections.getTypesAnnotatedWith(MAI1.class, true), annotatedWith(MAI1.class));

        assertThat(reflections.getTypesAnnotatedWith(AI2.class, true), are(I2.class));
        assertThat(reflections.getTypesAnnotatedWith(AI2.class, true), annotatedWith(AI2.class));

        assertThat(reflections.getTypesAnnotatedWith(AC1.class, true), are(C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(AC1.class, true), annotatedWith(AC1.class));

        assertThat(reflections.getTypesAnnotatedWith(AC1n.class, true), are(C1.class));
        assertThat(reflections.getTypesAnnotatedWith(AC1n.class, true), annotatedWith(AC1n.class));
        assertThat(reflections.getTypesAnnotatedWith(MAI1.class), are(AI1.class, I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(AI1.class), are(I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(AI2.class), are(I2.class, C1.class, C2.class, C3.class, C5.class));

        assertThat(reflections.getTypesAnnotatedWith(AM1.class), isEmpty);

        //annotation member value matching
        AC2 ac2 = new AC2() {
            public String value() {return "ac2";}
            public Class<? extends Annotation> annotationType() {return AC2.class;}};

        assertThat(reflections.getTypesAnnotatedWith(ac2), are(C3.class, C5.class, I3.class, C6.class, AC3.class, C7.class));

        assertThat(reflections.getTypesAnnotatedWith(ac2, true), are(C3.class, I3.class, AC3.class));
    }

    @Test
    public void testMethodsAnnotatedWith() throws NoSuchMethodException {
        assertThat(reflections.getMethodsAnnotatedWith(AM1.class),
                are(C4.class.getDeclaredMethod("m1"),
                    C4.class.getDeclaredMethod("m1", int.class, String[].class),
                    C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                    C4.class.getDeclaredMethod("m3")));

        AM1 am1 = new AM1() {
            public String value() {return "1";}
            public Class<? extends Annotation> annotationType() {return AM1.class;}
        };
        assertThat(reflections.getMethodsAnnotatedWith(am1),
                are(C4.class.getDeclaredMethod("m1"),
                    C4.class.getDeclaredMethod("m1", int.class, String[].class),
                    C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));
    }

    @Test
    public void testConstructorsAnnotatedWith() throws NoSuchMethodException {
        assertThat(reflections.getConstructorsAnnotatedWith(AM1.class),
                are(C4.class.getDeclaredConstructor(String.class)));

        AM1 am1 = new AM1() {
            public String value() {return "1";}
            public Class<? extends Annotation> annotationType() {return AM1.class;}
        };
        assertThat(reflections.getConstructorsAnnotatedWith(am1),
                are(C4.class.getDeclaredConstructor(String.class)));
    }

    @Test
    public void testFieldsAnnotatedWith() throws NoSuchFieldException {
        assertThat(reflections.getFieldsAnnotatedWith(AF1.class),
                are(C4.class.getDeclaredField("f1"),
                    C4.class.getDeclaredField("f2")
                    ));

        assertThat(reflections.getFieldsAnnotatedWith(new AF1() {
                        public String value() {return "2";}
                        public Class<? extends Annotation> annotationType() {return AF1.class;}}),
                are(C4.class.getDeclaredField("f2")));
    }

    @Test
    public void testMethodParameter() throws NoSuchMethodException {
            assertThat(reflections.getMethodsWithParameter(String.class),
                    are(C4.class.getDeclaredMethod("m4", String.class), UsageTestModel.C1.class.getDeclaredMethod("method", String.class)));

            assertThat(reflections.getMethodsWithSignature(),
                    are(C4.class.getDeclaredMethod("m1"), C4.class.getDeclaredMethod("m3"),
                            AC2.class.getMethod("value"), AF1.class.getMethod("value"), AM1.class.getMethod("value"),
                            UsageTestModel.C1.class.getDeclaredMethod("method"), UsageTestModel.C2.class.getDeclaredMethod("method")));

            assertThat(reflections.getMethodsWithSignature(int[][].class, String[][].class),
                    are(C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

            assertThat(reflections.getMethodsReturn(int.class),
                    are(C4.class.getDeclaredMethod("add", int.class, int.class)));

            assertThat(reflections.getMethodsReturn(String.class),
                    are(C4.class.getDeclaredMethod("m3"), C4.class.getDeclaredMethod("m4", String.class),
                            AC2.class.getMethod("value"), AF1.class.getMethod("value"), AM1.class.getMethod("value")));

            assertThat(reflections.getMethodsReturn(void.class),
                    are(C4.class.getDeclaredMethod("m1"), C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            C4.class.getDeclaredMethod("m1", int[][].class, String[][].class), UsageTestModel.C1.class.getDeclaredMethod("method"),
                            UsageTestModel.C1.class.getDeclaredMethod("method", String.class), UsageTestModel.C2.class.getDeclaredMethod("method")));

            assertThat(reflections.getMethodsWithParameter(AM1.class),
                    are(C4.class.getDeclaredMethod("m4", String.class)));

            assertThat(reflections.getMethodsWithParameter(AM2.class),
                    are(C4.class.getDeclaredMethod("m4", String.class),
                            C4.class.getDeclaredMethod("m1", int.class, String[].class)));
    }

    @Test
    public void testConstructorParameter() throws NoSuchMethodException {
        assertThat(reflections.getConstructorsWithParameter(String.class),
                are(C4.class.getDeclaredConstructor(String.class)));

        assertThat(reflections.getConstructorsWithSignature(),
                are(C1.class.getDeclaredConstructor(), C2.class.getDeclaredConstructor(), C3.class.getDeclaredConstructor(),
                        C4.class.getDeclaredConstructor(), C5.class.getDeclaredConstructor(), C6.class.getDeclaredConstructor(),
                        C7.class.getDeclaredConstructor(), UsageTestModel.C1.class.getDeclaredConstructor(), UsageTestModel.C2.class.getDeclaredConstructor()));

        assertThat(reflections.getConstructorsWithParameter(AM1.class),
                are(C4.class.getDeclaredConstructor(String.class)));
    }

    @Test
    public void testResourcesScanner() {
        Predicate<String> filter = new FilterBuilder().includePattern(".*\\.xml").excludePattern(".*testModel-reflections\\.xml");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(Scanners.Resources)
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class))));

        Collection<String> resolved = reflections.getResources(Pattern.compile(".*resource1-reflections\\.xml"));
        assertThat(resolved, are("META-INF/reflections/resource1-reflections.xml"));

        Collection<String> resources = reflections.getResources(".*");
        assertThat(resources, are("META-INF/reflections/resource1-reflections.xml", "META-INF/reflections/inner/resource2-reflections.xml"));
    }

    @Test
    public void testMethodParameterNames() throws NoSuchMethodException {
        assertEquals(reflections.getMemberParameterNames(C4.class.getDeclaredMethod("m3")),
                Collections.emptyList());

        assertEquals(reflections.getMemberParameterNames(C4.class.getDeclaredMethod("m4", String.class)),
                Collections.singletonList("string"));

        assertEquals(reflections.getMemberParameterNames(C4.class.getDeclaredMethod("add", int.class, int.class)),
                Arrays.asList("i1", "i2"));

        assertEquals(reflections.getMemberParameterNames(C4.class.getDeclaredConstructor(String.class)),
                Collections.singletonList("f1"));
    }

    @Test
    public void testMemberUsageScanner() throws NoSuchFieldException, NoSuchMethodException {
        //field usage
        assertThat(reflections.getMemberUsage(UsageTestModel.C1.class.getDeclaredField("c2")),
                are(UsageTestModel.C1.class.getDeclaredConstructor(),
                        UsageTestModel.C1.class.getDeclaredConstructor(UsageTestModel.C2.class),
                        UsageTestModel.C1.class.getDeclaredMethod("method"),
                        UsageTestModel.C1.class.getDeclaredMethod("method", String.class)));

        //method usage
        assertThat(reflections.getMemberUsage(UsageTestModel.C1.class.getDeclaredMethod("method")),
                are(UsageTestModel.C2.class.getDeclaredMethod("method")));

        assertThat(reflections.getMemberUsage(UsageTestModel.C1.class.getDeclaredMethod("method", String.class)),
                are(UsageTestModel.C2.class.getDeclaredMethod("method")));

        //constructor usage
        assertThat(reflections.getMemberUsage(UsageTestModel.C1.class.getDeclaredConstructor()),
                are(UsageTestModel.C2.class.getDeclaredConstructor(),
                        UsageTestModel.C2.class.getDeclaredMethod("method")));

        assertThat(reflections.getMemberUsage(UsageTestModel.C1.class.getDeclaredConstructor(UsageTestModel.C2.class)),
                are(UsageTestModel.C2.class.getDeclaredMethod("method")));
    }

    @Test
    public void testScannerNotConfigured() throws NoSuchMethodException {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
            .filterInputsBy(TestModelFilter.includePackage("org\\.reflections\\.UsageTestModel\\$.*"))
            .setScanners());

        assertTrue(reflections.getSubTypesOf(C1.class).isEmpty());
        assertTrue(reflections.getTypesAnnotatedWith(AC1.class).isEmpty());
        assertTrue(reflections.getMethodsAnnotatedWith(AC1.class).isEmpty());
        assertTrue(reflections.getMethodsWithSignature().isEmpty());
        assertTrue(reflections.getMethodsWithParameter(String.class).isEmpty());
        assertTrue(reflections.getMethodsReturn(String.class).isEmpty());
        assertTrue(reflections.getConstructorsAnnotatedWith(AM1.class).isEmpty());
        assertTrue(reflections.getConstructorsWithSignature().isEmpty());
        assertTrue(reflections.getConstructorsWithParameter(String.class).isEmpty());
        assertTrue(reflections.getFieldsAnnotatedWith(AF1.class).isEmpty());
        assertTrue(reflections.getResources(".*").isEmpty());
        assertTrue(reflections.getMemberParameterNames(C4.class.getDeclaredMethod("m4", String.class)).isEmpty());
        assertTrue(reflections.getMemberUsage(UsageTestModel.C1.class.getDeclaredConstructor()).isEmpty());
        assertTrue(reflections.getAllTypes().isEmpty());
    }

    //
    public static String getUserDir() {
        File file = new File(System.getProperty("user.dir"));
        //a hack to fix user.dir issue(?) in surfire
        if (Arrays.asList(file.list()).contains("reflections")) {
            file = new File(file, "reflections");
        }
        return file.getAbsolutePath();
    }

    private final BaseMatcher<Collection<Class<?>>> isEmpty = new BaseMatcher<Collection<Class<?>>>() {
        public boolean matches(Object o) {
            return ((Collection<?>) o).isEmpty();
        }

        public void describeTo(Description description) {
            description.appendText("empty collection");
        }
    };

    private abstract static class Match<T> extends BaseMatcher<T> {
        public void describeTo(Description description) { }
    }

    public static <T> Matcher<Collection<? super T>> are(final T... ts) {
        final Collection<?> c1 = Arrays.asList(ts);
        return new Match<Collection<? super T>>() {
            public boolean matches(Object o) {
                Collection<?> c2 = (Collection<?>) o;
                return c1.containsAll(c2) && c2.containsAll(c1);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Arrays.toString(ts));
            }
        };
    }

    @SafeVarargs
    public static <T> Matcher<Collection<T>> equalTo(T... operand) {
        return IsEqual.equalTo(new HashSet<>(Arrays.asList(operand)));
    }

    @SafeVarargs
    public final <T extends AnnotatedElement> Matcher<Collection<String>> equalToNames(T... operand) {
        return IsEqual.equalTo(new HashSet<>(toNames(operand)));
    }

    private Matcher<Collection<Class<?>>> annotatedWith(final Class<? extends Annotation> annotation) {
        return new Match<Collection<Class<?>>>() {
            public boolean matches(Object o) {
                for (Class<?> c : (Iterable<Class<?>>) o) {
                    if (!annotationTypes(Arrays.asList(c.getAnnotations())).contains(annotation)) return false;
                }
                return true;
            }
        };
    }

    private List<Class<? extends Annotation>> annotationTypes(Collection<Annotation> annotations) {
        return annotations.stream().filter(Objects::nonNull).map(Annotation::annotationType).collect(Collectors.toList());
    }
}
