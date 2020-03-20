package org.reflections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.adapters.JavaReflectionAdapter;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Collections;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MetaReflectionsTest {
    public static final FilterBuilder TestModelFilter = new FilterBuilder().include("org.reflections.TestModel\\$.*");
    static Reflections metaEnabledReflections;

    @BeforeClass
    public static void init() {
        metaEnabledReflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(true),
                        new MethodAnnotationsScanner(true),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()
                ));
    }

    @Test
    public void testMetaAnnotatedMethods() {
        try {
            assertThat(metaEnabledReflections.getMethodsAnnotatedWith(TestModel.Annotation1.class),
                    ReflectionsTest.are(TestModel.MetaClass.class.getDeclaredMethod("testMethod1"),
                            TestModel.MetaClass.class.getDeclaredMethod("testMethod2")
                    ));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }



    @Test
    public void testMetaAnnotatedFields() {
        try {
            assertThat(metaEnabledReflections.getFieldsAnnotatedWith(TestModel.Annotation1.class),
                    ReflectionsTest.are(TestModel.MetaClass.class.getDeclaredField("testField1"),
                            TestModel.MetaClass.class.getDeclaredField("testField2")
                    ));
        } catch (NoSuchFieldException e) {
            fail();
        }
    }


}
