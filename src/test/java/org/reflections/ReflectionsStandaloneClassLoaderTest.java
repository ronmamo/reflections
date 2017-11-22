package org.reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.util.ConfigurationBuilder;

/**
 * The tests designed to check that Reflections is able to analyze classes that are not in current project's class loader.
 * One use case for this is Maven Plugin development: it has all info needed to create project's class loader,
 * but plugin's class loader is not supposed to be used in analysis.
 *
 * In order to achieve this, a standalone jar {@link ReflectionsStandaloneClassLoaderTest#RESOURCE_NAME} is created.
 * It contains a single class, {@link ReflectionsStandaloneClassLoaderTest#TEST_CLASS_NAME}.
 *
 * The code for the class is following:
 * <pre>
 *  package org.reflections;
 *
 *  public class TestClass {
 *      private final String testField;
 *
 *      public TestClass() {
 *          this("Default test field value");
 *      }
 *
 *      public TestClass(String testField) {
 *          this.testField = testField;
 *          printTestField();
 *      }
 *
 *      public void printTestField() {
 *          System.out.println(testField);
 *      }
 *  }
 * </pre>
 *
 * The class is not included into the project, since otherwise it will be included in its class path when tests are run
 * thus spoiling the tests.
 */
public class ReflectionsStandaloneClassLoaderTest {
    private static final String RESOURCE_NAME = "reflections-test-jar.jar";
    private static final String TEST_CLASS_NAME = "org.reflections.TestClass";
    private static final String TEST_METHOD_NAME = "printTestField";
    private static final String TEST_FIELD_NAME = "testField";

    private Reflections customClassLoaderReflections;
    private Class<?> testClass;

    @Before
    public void setUp() {
        URL standaloneUrl = this.getClass().getClassLoader().getResource(RESOURCE_NAME);
        Assert.assertNotNull(String.format("Unable to find resource '%s' in project's resources", RESOURCE_NAME), standaloneUrl);

        ClassLoader customClassLoader = new URLClassLoader(new URL[]{standaloneUrl});
        customClassLoaderReflections = new Reflections(
                new ConfigurationBuilder()
                        .addScanners(new MemberUsageScanner())
                        .addClassLoader(customClassLoader)
                        .addUrls(standaloneUrl)
        );

        try {
            testClass = customClassLoader.loadClass(TEST_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(String.format("Was unable to find class '%s' in jar '%s'", TEST_CLASS_NAME, RESOURCE_NAME), e);
        }
    }

    @Test
    public void testMethod() throws NoSuchMethodException, ClassNotFoundException {
        Method testMethod = testClass.getDeclaredMethod(TEST_METHOD_NAME);
        Set<Member> methodUsage = customClassLoaderReflections.getMethodUsage(testMethod);
        Assert.assertFalse(methodUsage.isEmpty());
    }

    @Test
    public void testConstructor() throws ClassNotFoundException, NoSuchMethodException {
        Constructor<?> testConstructor = testClass.getDeclaredConstructor(String.class);
        Set<Member> constructorUsages = customClassLoaderReflections.getConstructorUsage(testConstructor);
        Assert.assertFalse(constructorUsages.isEmpty());
    }

    @Test
    public void testField() throws ClassNotFoundException, NoSuchFieldException {
        Field testField = testClass.getDeclaredField(TEST_FIELD_NAME);
        Set<Member> fieldUsages = customClassLoaderReflections.getFieldUsage(testField);
        Assert.assertFalse(fieldUsages.isEmpty());
    }
}
