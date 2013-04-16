package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.serializers.JsonSerializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.vfs.Vfs;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.reflections.TestModel.AC1;
import static org.reflections.TestModel.AC2;
import static org.reflections.TestModel.AF1;
import static org.reflections.TestModel.AI1;
import static org.reflections.TestModel.AI2;
import static org.reflections.TestModel.AM1;
import static org.reflections.TestModel.C1;
import static org.reflections.TestModel.C2;
import static org.reflections.TestModel.C3;
import static org.reflections.TestModel.C4;
import static org.reflections.TestModel.C5;
import static org.reflections.TestModel.C6;
import static org.reflections.TestModel.I1;
import static org.reflections.TestModel.I2;
import static org.reflections.TestModel.I3;
import static org.reflections.TestModel.MAI1;

/**
 *
 */
public class ReflectionsTest {
    static Reflections reflections;

    @BeforeClass
    public static void init() {
        Predicate<String> filter = new FilterBuilder().include("org.reflections.TestModel\\$.*");
        reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(
                        new SubTypesScanner().filterResultsBy(filter),
                        new TypeAnnotationsScanner().filterResultsBy(filter),
                        new FieldAnnotationsScanner().filterResultsBy(filter),
                        new MethodAnnotationsScanner().filterResultsBy(filter),
                        new MethodParameterScanner())
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class))));
    }

    protected void testAll() {
        testSubTypesOf();
        testTypesAnnotatedWith();
        testMethodsAnnotatedWith();
        testConstructorsAnnotatedWith();
        testFieldsAnnotatedWith();
        testMethodParameter();
    }

    @Test
    public void testSubTypesOf() {
        assertThat(reflections.getSubTypesOf(I1.class), are(I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getSubTypesOf(I2.class), are(C1.class, C2.class, C3.class, C5.class));
    }

    @Test
    public void testTypesAnnotatedWith() {
        //@Inherited
        assertThat("when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types",
                reflections.getTypesAnnotatedWith(MAI1.class), are(AI1.class));

        assertThat("when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types",
                reflections.getTypesAnnotatedWith(AI2.class), are(I2.class));

        assertThat("when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types",
                reflections.getTypesAnnotatedWith(AC1.class), are(C1.class, C2.class, C3.class, C5.class));

        assertThat("when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes",
                reflections.getTypesAnnotatedWith(AI1.class, false), are(I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));

        assertThat("when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes",
                reflections.getTypesAnnotatedWith(MAI1.class, false), are(AI1.class, I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));

        assertThat("when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes",
                reflections.getTypesAnnotatedWith(AI2.class, false), are(I2.class, C1.class, C2.class, C3.class, C5.class));

        assertThat(reflections.getTypesAnnotatedWith(AM1.class), isEmpty);

        //annotation member value matching
        AC2 ac2 = new AC2() {
            public String value() {return "ugh?!";}
            public Class<? extends Annotation> annotationType() {return AC2.class;}};

        assertThat("when honoring @Inherited, meta-annotation should only effect annotated super classes and it's sub types",
                reflections.getTypesAnnotatedWith(ac2), are(C3.class, I3.class));

        assertThat("when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes",
                reflections.getTypesAnnotatedWith(ac2, false), are(C3.class, C5.class, I3.class, C6.class));
    }

    @Test
    public void testMethodsAnnotatedWith() {
        try {
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
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testConstructorsAnnotatedWith() {
        try {
            assertThat(reflections.getConstructorsAnnotatedWith(AM1.class),
                    are(C4.class.getDeclaredConstructor(String.class)));

            AM1 am1 = new AM1() {
                public String value() {return "1";}
                public Class<? extends Annotation> annotationType() {return AM1.class;}
            };
            assertThat(reflections.getConstructorsAnnotatedWith(am1),
                    are(C4.class.getDeclaredConstructor(String.class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
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

    @Test
    public void testMethodParameter() {
        try {
            assertThat(reflections.getMethodsMatchParams(String.class),
                    are(C4.class.getDeclaredMethod("m4", String.class)));

            assertThat(reflections.getMethodsMatchParams(),
                    are(C4.class.getDeclaredMethod("m1"), C4.class.getDeclaredMethod("m3"),
                            AC2.class.getMethod("value"), AF1.class.getMethod("value"), AM1.class.getMethod("value")));

            assertThat(reflections.getMethodsReturn(int.class),
                    are(C4.class.getDeclaredMethod("add", int.class, int.class)));

            assertThat(reflections.getMethodsReturn(String.class),
                    are(C4.class.getDeclaredMethod("m3"), C4.class.getDeclaredMethod("m4", String.class),
                            AC2.class.getMethod("value"), AF1.class.getMethod("value"), AM1.class.getMethod("value")));

            assertThat(reflections.getMethodsReturn(void.class),
                    are(C4.class.getDeclaredMethod("m1"),
                            C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

            assertThat(reflections.getMethodsWithAnyParamAnnotated(AM1.class),
                    are(C4.class.getDeclaredMethod("m4", String.class)));

            assertThat(reflections.getMethodsWithAnyParamAnnotated(
                    new AM1() {
                        public String value() { return "2"; }
                        public Class<? extends Annotation> annotationType() { return AM1.class; }
                    }),
                    are(C4.class.getDeclaredMethod("m4", String.class)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConstructorParameter() throws NoSuchMethodException {
        assertThat(reflections.getConstructorsMatchParams(String.class),
                are(C4.class.getDeclaredConstructor(String.class)));

        assertThat(reflections.getConstructorsMatchParams(),
                are(C1.class.getDeclaredConstructor(), C2.class.getDeclaredConstructor(), C3.class.getDeclaredConstructor(),
                        C4.class.getDeclaredConstructor(), C5.class.getDeclaredConstructor(), C6.class.getDeclaredConstructor()));

        assertThat(reflections.getConstructorsWithAnyParamAnnotated(AM1.class),
                are(C4.class.getDeclaredConstructor(String.class)));

        assertThat(reflections.getConstructorsWithAnyParamAnnotated(
                new AM1() {
                    public String value() { return "1"; }
                    public Class<? extends Annotation> annotationType() { return AM1.class; }
                }),
                are(C4.class.getDeclaredConstructor(String.class)));
    }

    @Test
    public void collect() {
        Predicate<String> filter = new FilterBuilder().include("org.reflections.TestModel\\$.*");
        Reflections testModelReflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(
                        new SubTypesScanner().filterResultsBy(filter),
                        new TypeAnnotationsScanner().filterResultsBy(filter),
                        new MethodParameterScanner())
                                .setUrls(asList(ClasspathHelper.forClass(TestModel.class))));

        String path = getUserDir() + "/target/test-classes" + "/META-INF/reflections/testModel-reflections.xml";
        testModelReflections.save(path);

        reflections = Reflections.collect();
        testAll();
    }

    @Test
    public void collectInputStream() {
        final Iterable<Vfs.File> xmls = Vfs.findFiles(Arrays.asList(ClasspathHelper.forClass(ReflectionsTest.class)), new Predicate<Vfs.File>() {
            public boolean apply(Vfs.File input) {
                return input.getName().endsWith(".xml");
            }
        });

        reflections = new Reflections();
        for (Vfs.File xml : xmls) {
            try {
                reflections.collect(xml.openInputStream());
            } catch (IOException e) {
                throw new RuntimeException("", e);
            }
        }

        testAll();
    }

    @Test
    public void jsonCollect() {
        Predicate<String> filter = new FilterBuilder().include("org.reflections.TestModel\\$.*");
        Reflections testModelReflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(
                        new SubTypesScanner().filterResultsBy(filter),
                        new TypeAnnotationsScanner().filterResultsBy(filter),
                        new MethodParameterScanner())
                                .setUrls(asList(ClasspathHelper.forClass(TestModel.class))));

        String path = getUserDir() + "/target/test-classes" + "/META-INF/reflections/testModel-reflections.json";
        
        final JsonSerializer serializer = new JsonSerializer();
        testModelReflections.save(path, serializer);

        reflections = Reflections.collect("META-INF/reflections",
                new FilterBuilder().include(".*-reflections.json"),
                serializer);

        reflections.merge(
                Reflections.collect("META-INF/reflections",
                        new FilterBuilder().include(".*-reflections.xml").exclude("testModel-reflections.xml"),
                        new XmlSerializer()));

        //todo what about duplicates?

        testAll();
    }

    public static String getUserDir() {
        File file = new File(System.getProperty("user.dir"));
        //a hack to fix user.dir issue(?) in surfire
        if (Lists.newArrayList(file.list()).contains("reflections")) {
            file = new File(file, "reflections");
        }
        return file.getAbsolutePath();
    }

    @Test
    public void testResourcesScanner() {
        Predicate<String> filter = new FilterBuilder().include(".*\\.xml");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new ResourcesScanner())
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class))));

        Set<String> resolved = reflections.getResources(Pattern.compile(".*resource1-reflections\\.xml"));
        Assert.assertThat(resolved, are("META-INF/reflections/resource1-reflections.xml"));

        Set<String> resources = reflections.getStore().get(ResourcesScanner.class).keySet();
        Assert.assertThat(resources, are(
                "resource1-reflections.xml", "resource2-reflections.xml", "testModel-reflections.xml"
        ));
    }

    //
    private final BaseMatcher<Set<Class<?>>> isEmpty = new BaseMatcher<Set<Class<?>>>() {
        public boolean matches(Object o) {
            return ((Collection<?>) o).isEmpty();
        }

        public void describeTo(Description description) {
            description.appendText("empty collection");
        }
    };

    public static <T> Matcher<Set<? super T>> are(final T... ts) {
        final Collection<?> c1 = Arrays.asList(ts);
        return new BaseMatcher<Set<? super T>>() {
            public boolean matches(Object o) {
                Collection<?> c2 = (Collection<?>) o;
                return c1.containsAll(c2) && c2.containsAll(c1);
            }

            public void describeTo(Description description) {
                description.appendText("elements: ");
                description.appendValueList("(", ",", ")", ts);
            }
        };
    }
}
