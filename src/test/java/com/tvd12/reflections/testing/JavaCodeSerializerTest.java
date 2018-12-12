package com.tvd12.reflections.testing;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tvd12.reflections.Reflections;
import com.tvd12.reflections.scanners.TypeElementsScanner;
import com.tvd12.reflections.serializers.JavaCodeSerializer;
import com.tvd12.reflections.util.ClasspathHelper;
import com.tvd12.reflections.util.ConfigurationBuilder;
import com.tvd12.reflections.util.FilterBuilder;

import static com.tvd12.reflections.testing.TestModel.AC2;
import static com.tvd12.reflections.testing.TestModel.C1;
import static com.tvd12.reflections.testing.TestModel.C2;
import static com.tvd12.reflections.testing.TestModel.C4;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.function.Predicate;

/** */
public class JavaCodeSerializerTest {

    @BeforeClass
    public static void generateAndSave() {
        Predicate<String> filter = new FilterBuilder().include("com.tvd12.reflections.testing.TestModel\\$.*");

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new TypeElementsScanner().includeFields().publicOnly(false))
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class))));

        //save
        String filename = ReflectionsTest.getUserDir() + "/src/test/java/com.tvd12.reflections.testing.MyTestModelStore";
        reflections.save(filename, new JavaCodeSerializer());
    }

    @Test
    public void resolve() throws NoSuchMethodException, NoSuchFieldException {
        //class
        assertEquals(C1.class,
                JavaCodeSerializer.resolveClass(MyTestModelStore.com.tvd12.reflections.testing.TestModel$C1.class));

        //method
        assertEquals(C4.class.getDeclaredMethod("m1"),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.com.tvd12.reflections.testing.TestModel$C4.methods.m1.class));

        //overloaded method with parameters
        assertEquals(C4.class.getDeclaredMethod("m1", int.class, String[].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.com.tvd12.reflections.testing.TestModel$C4.methods.m1_int__java_lang_String$$.class));

        //overloaded method with parameters and multi dimensional array
        assertEquals(C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.com.tvd12.reflections.testing.TestModel$C4.methods.m1_int$$$$__java_lang_String$$$$.class));

        //field
        assertEquals(C4.class.getDeclaredField("f1"),
                JavaCodeSerializer.resolveField(MyTestModelStore.com.tvd12.reflections.testing.TestModel$C4.fields.f1.class));

        //annotation
        assertEquals(C2.class.getAnnotation(AC2.class),
                JavaCodeSerializer.resolveAnnotation(MyTestModelStore.com.tvd12.reflections.testing.TestModel$C2.annotations.com_tvd12_reflections_testing_TestModel$AC2.class));
    }
}
