/*
 * (c) Copyright 2015 freiheit.com technologies GmbH
 *
 * Created on 13.08.2015 by Michael Bohn (michael.bohn@freiheit.com)
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies GmbH. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies GmbH.
 */
package org.reflections;

import java.util.Set;

import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import sun.misc.Launcher;

/**
 * Test reflect over JDK package.
 */
public class JdkPackageTest {
    @Test
    public void testReadFunctionsFromJDK() throws Exception {
        final Reflections reflections = new Reflections( "java.util.function", Launcher.getBootstrapClassPath().getURLs() );
        final Set<Class<?>> test = reflections.getTypesAnnotatedWith( FunctionalInterface.class );
        assertTrue( test.size() > 0 );
    }
}
