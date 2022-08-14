package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationBuilderTest {

	@Test
	public void buildForConfig() {
		assertConfig(ConfigurationBuilder.build("org.reflections"),
			ClasspathHelper.forPackage("org.reflections"),
			new FilterBuilder().includePackage("org.reflections"));

		assertConfig(ConfigurationBuilder.build("org"),
			ClasspathHelper.forPackage("org"),
			new FilterBuilder().includePackage("org"));
	}

	@Test
	public void buildFor() {
		assertThrows(ReflectionsException.class, () -> ConfigurationBuilder.build(""));

		assertConfig(ConfigurationBuilder.build(),
			ClasspathHelper.forClassLoader(),
			new FilterBuilder());

		assertConfig(ConfigurationBuilder.build("not.exist"),
			ClasspathHelper.forClassLoader(),
			new FilterBuilder().includePackage("not.exist"));
	}

	private void assertConfig(ConfigurationBuilder config, Collection<URL> urls, Predicate<String> inputsFilter) {
		assertEquals(config.getUrls(), new HashSet<>(urls));
		assertEquals(config.getInputsFilter(), inputsFilter);
		assertEquals(config.getScanners(), new HashSet<>(Arrays.asList(Scanners.SubTypes, Scanners.TypesAnnotated)));
	}
}