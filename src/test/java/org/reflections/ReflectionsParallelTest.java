package org.reflections;

import com.google.common.base.Predicate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.scanners.*;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.FilterBuilder;

import static java.util.Arrays.asList;

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
