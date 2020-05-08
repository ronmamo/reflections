package org.reflections;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.util.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.reflections.ReflectionUtils.*;
import static org.reflections.ReflectionsTest.are;

/**
 * @author mamo
 */
@SuppressWarnings("unchecked")
public class ReflectionUtilsTest {

    @Test
    public void getAllTest() {
        assertThat(getAllSuperTypes(TestModel.C3.class, withAnnotation(TestModel.AI1.class)), are(TestModel.I1.class));

        Set<Method> allMethods = getAllMethods(TestModel.C4.class, withModifier(Modifier.PUBLIC), withReturnType(void.class));
        Set<Method> allMethods1 = getAllMethods(TestModel.C4.class, withPattern("public.*.void .*"));

        assertTrue(allMethods.containsAll(allMethods1) && allMethods1.containsAll(allMethods));
        assertThat(allMethods1, names("m1"));

        assertThat(getAllMethods(TestModel.C4.class, withAnyParameterAnnotation(TestModel.AM1.class)), names("m4"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(TestModel.AF1.class)), names("f1", "f2"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(new TestModel.AF1() {
            public String value() {return "2";}
            public Class<? extends Annotation> annotationType() {return TestModel.AF1.class;}})),
                names("f2"));

        assertThat(getAllFields(TestModel.C4.class, withTypeAssignableTo(String.class)), names("f1", "f2", "f3"));

        assertThat(getAllConstructors(TestModel.C4.class, withParametersCount(0)), names(TestModel.C4.class.getName()));

        assertEquals(toStringSorted(getAllAnnotations(TestModel.C3.class)),
                "[@java.lang.annotation.Documented(), " +
                        "@java.lang.annotation.Inherited(), " +
                        "@java.lang.annotation.Retention(value=RUNTIME), " +
                        "@java.lang.annotation.Target(value=ANNOTATION_TYPE), " +
                        "@org.reflections.TestModel$AC1(), " +
                        "@org.reflections.TestModel$AC1n(), " +
                        "@org.reflections.TestModel$AC2(value=ugh?!), " +
                        "@org.reflections.TestModel$AI1(), " +
                        "@org.reflections.TestModel$AI2(), " +
                        "@org.reflections.TestModel$MAI1()]");

        Method m4 = getMethods(TestModel.C4.class, withName("m4")).iterator().next();
        assertEquals(m4.getName(), "m4");
        assertTrue(getAnnotations(m4).isEmpty());
    }

    @Test public void withParameter() throws Exception {
        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet<>();
        for (Class<?> type : getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(getAllMethods(target, withModifier(Modifier.STATIC), withParameters(type)));
        }

        Set<Method> allMethods1 = getAllMethods(target, withModifier(Modifier.STATIC), withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test
    public void withParametersAssignableFromTest() throws Exception {
        //Check for null safe
        getAllMethods(Collections.class, withModifier(Modifier.STATIC), withParametersAssignableFrom());

        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet<>();
        for (Class<?> type : getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(getAllMethods(target, withModifier(Modifier.STATIC), withParameters(type)));
        }

        Set<Method> allMethods1 = getAllMethods(target, withModifier(Modifier.STATIC), withParametersAssignableFrom(Iterable.class), withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test public void withReturn() {
        Set<Method> returnMember = getAllMethods(Class.class, withReturnTypeAssignableTo(Member.class));
        Set<Method> returnsAssignableToMember = getAllMethods(Class.class, withReturnType(Method.class));

        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));

        returnsAssignableToMember = getAllMethods(Class.class, withReturnType(Field.class));
        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));
    }

    @Test
    public void getAllAndReflections() {
        Reflections reflections = new Reflections(TestModel.class, new FieldAnnotationsScanner());

        Set<Field> af1 = reflections.getFieldsAnnotatedWith(TestModel.AF1.class);
        Set<? extends Field> allFields = ReflectionUtils.getAll(af1, withModifier(Modifier.PROTECTED));
        assertEquals(1, allFields.size());
        assertThat(allFields, names("f2"));
    }

    @Test
    public void testGetMemberFromDescriptor() throws NoSuchMethodException, NoSuchFieldException {
        // field
        Member c1Field = Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C1.c2",
                TestModel.class.getClassLoader());
        assertEquals(TestModel.Usage.C1.class.getDeclaredField("c2"), c1Field);

        // constructor
        Member c1Constructor = Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C1.<init>(org.reflections.TestModel$Usage$C2)",
                TestModel.class.getClassLoader());
        assertEquals(TestModel.Usage.C1.class.getConstructor(TestModel.Usage.C2.class), c1Constructor);

        // method
        Member c2Method = Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C2.method()",
                TestModel.class.getClassLoader());
        assertEquals(TestModel.Usage.C2.class.getDeclaredMethod("method"), c2Method);

        // synthetic method for lambda expression
        Member c2Lambda = Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C2.lambda$useLambda$0(org.reflections.TestModel$Usage$C1)",
                TestModel.class.getClassLoader());
        assertEquals(TestModel.Usage.C2.class.getDeclaredMethod("lambda$useLambda$0", TestModel.Usage.C1.class), c2Lambda);

        // method of anonymous inner class
        Member anonymousClassMethod = Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C2$1.applyAsDouble(org.reflections.TestModel$Usage$C1)",
                TestModel.class.getClassLoader());
        assertEquals(anonymousClassMethod.getName(), "applyAsDouble");
    }

    @Test(expected = ReflectionsException.class)
    public void testGetMemberFromDescriptorFieldNotFound() {
        Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C1.fieldNotExist");
    }

    @Test(expected = ReflectionsException.class)
    public void testGetMemberFromDescriptorConstructorNotFound() {
        Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C1.<init>(int)");
    }

    @Test(expected = ReflectionsException.class)
    public void testGetMemberFromDescriptorMethodNotFound() {
        Utils.getMemberFromDescriptor("org.reflections.TestModel$Usage$C1.methodNotExist()");
    }

    private Set<String> names(Set<? extends Member> o) {
        return o.stream().map(Member::getName).collect(Collectors.toSet());
    }

    private BaseMatcher<Set<? extends Member>> names(final String... namesArray) {
        return new BaseMatcher<Set<? extends Member>>() {

                public boolean matches(Object o) {
                    Collection<String> transform = names((Set<Member>) o);
                    final Collection<?> names = Arrays.asList(namesArray);
                    return transform.containsAll(names) && names.containsAll(transform);
                }

                public void describeTo(Description description) {
                }
            };
    }

    public static String toStringSorted(Set<?> set) {
        return set.stream()
                .map(o -> o.toString().replace("[", "").replace("]", "").replace("{", "").replace("}", "").replace("\"", ""))
                .sorted().collect(Collectors.toList()).toString();
    }
}
