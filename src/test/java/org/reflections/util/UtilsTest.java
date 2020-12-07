package org.reflections.util;

import java.lang.reflect.Member;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;
import org.reflections.ReflectionsException;
import org.reflections.TestModel;
import org.reflections.TestModel.C8;

public class UtilsTest {

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

    @Test
    public void testGetMemberFromDescriptorStringClassLoaderArray() {
        Method[] declaredMethods = C8.class.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String expectedString = method.toString();
            String actualString = Utils.getMemberFromDescriptor(expectedString).toString();
            assertEquals(expectedString, actualString);
        }
    }
}