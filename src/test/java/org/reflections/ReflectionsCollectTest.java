package org.reflections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reflections.scanners.Scanners;
import org.reflections.serializers.JsonSerializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
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
		assertFalse(reflections.getStore().isEmpty());

		// xml
		reflections.save(targetDir + "/META-INF/reflections/saved-testModel-reflections.xml");
		assertEquals(
			reflections.getStore(),
			Reflections.collect("/META-INF/reflections/testModel-reflections.xml", a -> true).getStore());
		assertEquals(
			Reflections.collect("/META-INF/reflections/testModel-reflections.xml", a -> true).getStore(),
			Reflections.collect("/META-INF/reflections/saved-testModel-reflections.xml", a -> true).getStore());

		// json
		reflections.save(targetDir + "/META-INF/reflections/saved-testModel-reflections.json", new JsonSerializer());
		assertEquals(
			reflections.getStore(),
			Reflections.collect("/META-INF/reflections/testModel-reflections.json", a -> true).getStore());
		assertEquals(
			Reflections.collect("/META-INF/reflections/testModel-reflections.json", a -> true).getStore(),
			Reflections.collect("/META-INF/reflections/saved-testModel-reflections.json", a -> true).getStore());

		// default collect()
		Reflections collected = Reflections.collect();
		assertFalse(collected.getStore().isEmpty());
		assertTrue(collected.getSubTypesOf(TestModel.C1.class).containsAll(
			Arrays.asList(TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)));
	}

	@Test
	public void testAndroidGradleBuildAndCollect(@TempDir Path tempDir) throws Exception {
		// 1. Package TestModel classes into a directory-less JAR (like Gradle jar outputs)
		Path testClassesPkgDir = new File(ClasspathHelper.forClass(TestModel.class).toURI())
			.toPath().resolve("org/reflections");
		Path classesJar = tempDir.resolve("classes.jar");
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(classesJar));
			 Stream<Path> classFiles = Files.list(testClassesPkgDir)) {
			List<Path> modelClasses = classFiles
				.filter(p -> p.getFileName().toString().matches("TestModel(\\$.*)?\\.class"))
				.collect(Collectors.toList());
			for (Path classFile : modelClasses) {
				addZipFileEntry(zos, "org/reflections/" + classFile.getFileName(), Files.readAllBytes(classFile));
			}
		}

		// 2. Build-time Gradle scan using URLClassLoader passed to Reflections(pkg, classLoader)
		Path savedXml = tempDir.resolve("build/META-INF/reflections/android-reflections.xml");
		Path savedJson = tempDir.resolve("build/META-INF/reflections/android-reflections.json");
		try (URLClassLoader jarClassLoader = new URLClassLoader(
			new URL[]{classesJar.toUri().toURL()}, null)) {
			assertFalse(jarClassLoader.getResources("org/reflections").hasMoreElements());

			Reflections buildTime = new Reflections("org.reflections", jarClassLoader);
			Class<?> c1 = jarClassLoader.loadClass(TestModel.C1.class.getName());
			Class<? extends java.lang.annotation.Annotation> ac1 =
				jarClassLoader.loadClass(TestModel.AC1.class.getName()).asSubclass(java.lang.annotation.Annotation.class);
			Set<String> expectedSubTypes = new HashSet<>(Arrays.asList(
				TestModel.C2.class.getName(), TestModel.C3.class.getName(), TestModel.C5.class.getName()));
			assertEquals(expectedSubTypes,
				buildTime.getSubTypesOf(c1).stream().map(Class::getName).collect(Collectors.toSet()));
			assertTrue(buildTime.getTypesAnnotatedWith(ac1).stream().map(Class::getName).collect(Collectors.toSet())
				.containsAll(expectedSubTypes));

			buildTime.save(savedXml.toString());
			buildTime.save(savedJson.toString(), new JsonSerializer());
		}

		// 3. Package serialized metadata into a directory-less .apk archive
		Path apkFile = tempDir.resolve("app-debug.apk");
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(apkFile))) {
			addZipFileEntry(zos, "AndroidManifest.xml", "<manifest package=\"org.reflections\"/>".getBytes(StandardCharsets.UTF_8));
			addZipFileEntry(zos, "META-INF/reflections/android-reflections.xml", Files.readAllBytes(savedXml));
			addZipFileEntry(zos, "META-INF/reflections/android-reflections.json", Files.readAllBytes(savedJson));
		}

		// 4. Collect at runtime using a non-URLClassLoader (mimicking Android's PathClassLoader)
		try (URLClassLoader delegate = new URLClassLoader(
			new URL[]{apkFile.toUri().toURL()}, null)) {
			ClassLoader androidClassLoader = new ClassLoader(null) {
				@Override
				protected Class<?> findClass(String name) throws ClassNotFoundException {
					return Class.forName(name, false, TestModel.class.getClassLoader());
				}

				@Override
				public Enumeration<URL> getResources(String name) throws IOException {
					return delegate.getResources(name);
				}

				@Override
				public URL getResource(String name) {
					return delegate.getResource(name);
				}
			};
			assertFalse(androidClassLoader.getResources("META-INF/reflections/").hasMoreElements());

			Reflections xmlCollected = Reflections.collect("META-INF/reflections/",
				new FilterBuilder().includePattern(".*-reflections\\.xml"), new XmlSerializer(), androidClassLoader);
			assertEquals(new HashSet<>(Arrays.asList(TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)),
				xmlCollected.getSubTypesOf(TestModel.C1.class));
			assertEquals(new HashSet<>(Arrays.asList(TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)),
				xmlCollected.getTypesAnnotatedWith(TestModel.AC1.class));

			Reflections jsonCollected = Reflections.collect("META-INF/reflections/",
				new FilterBuilder().includePattern(".*-reflections\\.json"), new JsonSerializer(), androidClassLoader);
			assertEquals(new HashSet<>(Arrays.asList(TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)),
				jsonCollected.getSubTypesOf(TestModel.C1.class));
		}
	}

	private static void addZipFileEntry(ZipOutputStream zos, String name, byte[] bytes) throws IOException {
		zos.putNextEntry(new ZipEntry(name));
		zos.write(bytes);
		zos.closeEntry();
	}
}
