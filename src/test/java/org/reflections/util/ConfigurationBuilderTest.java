package org.reflections.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationBuilderTest
{
    @Test
    public void testCallingAddClassLoaderMoreThanOnce()
    {
        ClassLoader fooClassLoader = new ClassLoader() { };
        ClassLoader barClassLoader = new ClassLoader() { };

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .addClassLoader( fooClassLoader  );

        // Attempt to add a second class loader
        configurationBuilder.addClassLoader( barClassLoader );
        assertTrue( true );
    }
}