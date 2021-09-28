package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.serializers.JsonSerializer;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.reflections.ReflectionsTest.getUserDir;

public class ReflectionsCollectTest {

	@Test
	public void testCollect() {
		Reflections reflections = new Reflections(
			new ConfigurationBuilder()
				.forPackage("org.reflections")
				.filterInputsBy(new FilterBuilder()
					.includePattern("org\\.reflections\\.TestModel\\$.*")
					.includePattern(".*\\.xml"))
				.addScanners(Scanners.values()));

		String targetDir = getUserDir() + "/target/test-classes";

		// xml
		reflections.save(targetDir + "/META-INF/reflections/saved-testModel-reflections.xml");
		assertEquals(
			Reflections.collect("/META-INF/reflections/testModel-reflections.xml", a -> true).getStore(),
			Reflections.collect("/META-INF/reflections/saved-testModel-reflections.xml", a -> true).getStore());

		// json
		reflections.save(targetDir + "/META-INF/reflections/saved-testModel-reflections.json", new JsonSerializer());
		assertEquals(
			Reflections.collect("/META-INF/reflections/testModel-reflections.json", a -> true).getStore(),
			Reflections.collect("/META-INF/reflections/saved-testModel-reflections.json", a -> true).getStore());
	}
}
