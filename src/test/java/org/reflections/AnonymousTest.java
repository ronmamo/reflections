package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Constructor;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.reflections.scanners.Scanners.SubTypes;

public class AnonymousTest {
    @Test
    public void testFindAnnotatedClasses() throws Exception {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(DemoUsageService.class.getPackage().getName()))
                        .filterInputsBy(new FilterBuilder().includePattern("org\\.reflections\\.DemoUsageService\\$.*"))
                        .setScanners(Scanners.values())
        );
        Set<Class<?>> subTypes = reflections.get(SubTypes.of(MySuperClass.class).asClass());
        for (Class clazz:subTypes) {
            Constructor cons = clazz.getDeclaredConstructor(DemoUsageService.class);
            Object inst = cons.newInstance(DemoUsageService.class.getDeclaredConstructor().newInstance());
            assertNotEquals("MySuperClass", ((MySuperClass) inst).getClassName());
        }
    }
}