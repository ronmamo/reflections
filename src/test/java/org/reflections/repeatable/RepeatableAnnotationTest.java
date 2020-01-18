package org.reflections.repeatable;

import org.junit.Test;
import org.reflections.Reflections;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepeatableAnnotationTest {

    @Test
    public void test() {
        Reflections ref = new Reflections(RepeatableAnnotationTest.class.getPackage().getName());
        Set<Class<?>> clazzes = ref.getTypesAnnotatedWith(Name.class);
        assertTrue(clazzes.contains(SingleName.class));
        assertFalse(clazzes.contains(MultiName.class));

        clazzes = ref.getTypesAnnotatedWith(Names.class);
        assertFalse(clazzes.contains(SingleName.class));
        assertTrue(clazzes.contains(MultiName.class));
    }
}
