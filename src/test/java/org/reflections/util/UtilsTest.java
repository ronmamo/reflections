package org.reflections.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;
import org.reflections.TestModel.C8;

public class UtilsTest {

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
