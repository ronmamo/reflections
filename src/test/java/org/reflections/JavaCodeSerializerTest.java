package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.serializers.JavaCodeSerializer;
import org.reflections.util.FilterBuilder;
import org.reflections.util.NameHelper;

public class JavaCodeSerializerTest implements NameHelper {

	public JavaCodeSerializerTest() {
		FilterBuilder filterBuilder = new FilterBuilder().includePattern("org\\.reflections\\.TestModel\\$.*");
		Reflections reflections = new Reflections(
			TestModel.class,
			new TypeElementsScanner().filterResultsBy(filterBuilder),
			filterBuilder);

		String filename = ReflectionsTest.getUserDir() + "/src/test/java/org.reflections.MyTestModelStore";
		reflections.save(filename, new JavaCodeSerializer());
	}

	@Test
	public void check() {
		// MyTestModelStore contains TestModel type elements
		Class<?> c1 = MyTestModelStore.org.reflections.TestModel$C1.class;
		Class<?> ac1 = MyTestModelStore.org.reflections.TestModel$C1.annotations.org_reflections_TestModel$AC1.class;
		Class<?> f1 = MyTestModelStore.org.reflections.TestModel$C4.fields.f1.class;
		Class<?> m1 = MyTestModelStore.org.reflections.TestModel$C4.methods.m1.class;
	}
}
