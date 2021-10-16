package org.reflections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.UtilQueryBuilder;
import org.reflections.vfs.Vfs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.reflections.ReflectionUtils.get;

/**
 * test reflection symmetry between jrt scanned metadata (Scanners) and java reflection accessibility (ReflectionUtils functions).
 * <p>except for known differences per jdk version, these pairs should access similar metadata:
 * SubTypes/SuperTypes, TypesAnnotated/AnnotatedTypes, MethodsAnnotated/AnnotatedTypes, Resources etc...
 * <p></p>tested with AdoptOpenJDK
 */
@SuppressWarnings({"ArraysAsListWithZeroOrOneArgument"})
public class JdkTests {

	private final URL urls = ClasspathHelper.forClass(Object.class);

	@BeforeAll
	static void initJrtUrlType() {
		if (!Vfs.getDefaultUrlTypes().get(0).getClass().equals(JrtUrlType.class)) {
			Vfs.addDefaultURLTypes(new JrtUrlType());
		}
	}

	@Test
	public void checkSubTypesAndSuperTypes() {
		Map<String, Set<String>> diff = reflect(
			Scanners.SubTypes,
			ReflectionUtils.SuperTypes,
			Class.class);

		assertEquals(diff, Collections.emptyMap());
	}

	@Test
	public void checkTypesAnnotatedAndAnnotationTypes() {
		Map<String, Set<String>> diff = reflect(
			Scanners.TypesAnnotated,
			ReflectionUtils.AnnotationTypes,
			Class.class);

		Arrays.asList("jdk.internal.PreviewFeature", // jdk 15
				"jdk.internal.javac.PreviewFeature") // jdk 17
			.forEach(diff::remove);
		assertEquals(diff, Collections.emptyMap());
	}

	@Test
	public void checkMethodsAnnotatedAndAnnotationTypes() {
		Map<String, Set<String>> diff = reflect(
			Scanners.MethodsAnnotated,
			ReflectionUtils.AnnotationTypes,
			Method.class);

		// todo fix differences @A2 such as - @A1 public @A2 result method(...)
		Arrays.asList("com.sun.istack.internal.NotNull", // jdk 8
				"com.sun.istack.internal.Nullable",
				"sun.reflect.CallerSensitive",
				"java.lang.invoke.LambdaForm$Hidden",
				"jdk.internal.reflect.CallerSensitive",  // jdk 11, 13, 15
				"jdk.internal.PreviewFeature")           // jdk 15
			.forEach(diff::remove);
		assertEquals(diff, Collections.emptyMap());
	}

	@Test
	public void checkConstructorsAnnotatedAndAnnotationTypes() {
		Map<String, Set<String>> diff = reflect(
			Scanners.ConstructorsAnnotated,
			ReflectionUtils.AnnotationTypes,
			Constructor.class);

		assertEquals(diff, Collections.emptyMap());
	}

	@Test
	public void checkFieldsAnnotatedAndAnnotationTypes() {
		Map<String, Set<String>> diff = reflect(
			Scanners.FieldsAnnotated,
			ReflectionUtils.AnnotationTypes,
			Field.class);

		Arrays.asList("com.sun.istack.internal.NotNull", // jdk 8
				"com.sun.istack.internal.Nullable",
				"jdk.internal.PreviewFeature",           // jdk 15
				"jdk.internal.vm.annotation.Stable")     // jdk 17
			.forEach(diff::remove);
		assertEquals(diff, Collections.emptyMap());
	}

	@Test
	public void checkResources() {
		Reflections reflections = new Reflections(
			new ConfigurationBuilder()
				.addUrls(urls)
				.addScanners(Scanners.Resources));

		Set<String> diff = new HashSet<>();
		reflections.getStore().get(Scanners.Resources.index())
			.values().forEach(resources ->
				resources.forEach(resource -> {
					Set<URL> urls = get(ReflectionUtils.Resources.get(resource));
					for (URL url : urls) {
						try { if (!Files.exists(JrtUrlType.getJrtRealPath(url))) diff.add(resource); }
						catch (Exception e) { diff.add(resource); }
					}
				}));

		Arrays.asList("META-INF/MANIFEST.MF") // jdk 8
			.forEach(diff::remove);
		assertEquals(diff, Collections.emptySet());
	}

	@Test
	public void checkMethodsSignature() {
//        Map<String, Set<String>> diffMethodSignature =
//            findDiff(reflections, Scanners.MethodsSignature, ReflectionUtils.MethodSignature, Field.class);
//        assertEquals(diffMethodSignature, Collections.emptyMap());    }
	}

	private <F extends AnnotatedElement, E extends AnnotatedElement> Map<String, Set<String>> reflect(
		Scanner scanner, UtilQueryBuilder<F, E> utilQueryBuilder, Class<? extends AnnotatedElement> resultType) {
		System.out.print(scanner.index());
		measure("before");

		Reflections reflections = new Reflections(
			new ConfigurationBuilder()
				.addUrls(urls)
				.addScanners(scanner));
		measure("scan");

		Map<String, Set<String>> diffMap = findDiff(reflections, scanner, utilQueryBuilder, resultType);
		measure("query");

		reflections.getStore().clear();
		measure("clear");
		System.out.println();

		return diffMap;
	}

	private <F extends AnnotatedElement, E extends AnnotatedElement> Map<String, Set<String>> findDiff(
		Reflections reflections, Scanner scanner, UtilQueryBuilder<F, E> reflectionUtilsFunction, Class<? extends AnnotatedElement> resultType) {
		Map<String, Set<String>> missing = new HashMap<>();
		Map<String, Set<String>> mmap = reflections.getStore().get(scanner.index());
		assertFalse(mmap.isEmpty());
		mmap.forEach((key, strings) ->
			strings.forEach(string -> {
				//noinspection unchecked
				F element = (F) reflections.forName(string, resultType);
				if (element == null || !reflections.toNames(get(reflectionUtilsFunction.get(element))).contains(key)) {
					missing.computeIfAbsent(key, k -> new HashSet<>()).add(string);
				}
			}));
		return missing;
	}

	private void measure(String s) {
		System.out.printf(" -> %s %s", s, kb(mem()));
		gc();
		System.out.printf(" (gc -> %s)", kb(mem()));
	}

	private void gc() {
		for (int i = 0; i < 3; i++) {
			Runtime.getRuntime().gc();
			System.runFinalization();
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) { /*java sucks*/ }
		}
	}

	private long mem() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	private String kb(long mem2) {
		return (mem2 / 1024) + "kb";
	}

	public static class JrtUrlType implements Vfs.UrlType {
		@Override
		public boolean matches(URL url) throws Exception {
			return url.getProtocol().equals("jrt");
		}

		@Override
		public Vfs.Dir createDir(URL url) throws Exception {
			final Path realPath = getJrtRealPath(url);
			return new Vfs.Dir() {
				@Override
				public String getPath() {
					return url.getPath();
				}

				@Override
				public Iterable<Vfs.File> getFiles() {
					return () -> {
						try {
							return Files.walk(realPath)
								.filter(Files::isRegularFile)
								.map(p -> (Vfs.File) new Vfs.File() {
									@Override
									public String getName() {
										return p.toString();
									}

									@Override
									public String getRelativePath() {
										return p.startsWith(realPath) ? p.toString().substring(realPath.toString().length()) : p.toString();
									}

									@Override
									public InputStream openInputStream() throws IOException {
										return Files.newInputStream(p);
									}
								})
								.iterator();
						} catch (Exception e) {
							throw new ReflectionsException(e);
						}
					};
				}
			};
		}

		/**
		 * jdk 11 workaround for {@code Paths.get().toRealPath()}
		 */
		public static Path getJrtRealPath(URL url) throws IOException {
			// jdk 11 workaround
			return FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", url.getPath())
				.toRealPath();
		}
	}
}
