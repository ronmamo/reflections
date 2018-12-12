package com.tvd12.reflections.testing;

import static java.util.Arrays.asList;

import org.junit.BeforeClass;

import com.tvd12.reflections.Reflections;
import com.tvd12.reflections.scanners.FieldAnnotationsScanner;
import com.tvd12.reflections.scanners.MemberUsageScanner;
import com.tvd12.reflections.scanners.MethodAnnotationsScanner;
import com.tvd12.reflections.scanners.MethodParameterNamesScanner;
import com.tvd12.reflections.scanners.MethodParameterScanner;
import com.tvd12.reflections.scanners.SubTypesScanner;
import com.tvd12.reflections.scanners.TypeAnnotationsScanner;
import com.tvd12.reflections.util.ClasspathHelper;
import com.tvd12.reflections.util.ConfigurationBuilder;

/** */
public class ReflectionsParallelTest extends ReflectionsTest {

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(asList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner())
                .useParallelExecutor());
    }
}
