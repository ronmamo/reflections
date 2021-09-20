package org.reflections;

import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.FilterBuilder;
import org.reflections.util.NameHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withAnyParameterAnnotation;
import static org.reflections.TestModel.*;
import static org.reflections.scanners.Scanners.*;

public class ReflectionsQueryTest implements NameHelper {
	static Reflections reflections;
	private static ClassLoader[] loaders;

	public ReflectionsQueryTest() {
		reflections = new Reflections(
			TestModel.class,
			Scanners.values(),
			new FilterBuilder().includePattern("org\\.reflections\\.TestModel\\$.*")
				.or(s -> s.endsWith(".xml")));
		loaders = reflections.loaders();
	}

	@Test
	public void testSubTypes() {
		assertThat(reflections.get(SubTypes.of(I1.class)),
			equalToNames(I2.class, C1.class, C2.class, C3.class, C5.class));

		assertThat(reflections.get(SubTypes.of(C1.class)),
			equalToNames(C2.class, C3.class, C5.class));
	}

	@Test
	public void testTypesAnnotated() {
		assertThat(reflections.get(SubTypes.of(TypesAnnotated.with(MAI1.class))),
			equalToNames(AI1.class, I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));

		assertThat(reflections.get(SubTypes.of(TypesAnnotated.with(AI1.class))),
			equalToNames(I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));

		assertThat(reflections.get(SubTypes.of(TypesAnnotated.with(AI2.class))),
			equalToNames(I2.class, C1.class, C2.class, C3.class, C5.class));

		//annotation member value matching
		AC2 ac2 = new AC2() {
			public String value() { return "ac2"; }
			public Class<? extends Annotation> annotationType() { return AC2.class; }
		};

		assertThat(reflections.get(SubTypes.of(TypesAnnotated.of(
				TypesAnnotated.with(AC2.class).asClass(loaders)
					.filter(withAnnotation(ac2))
					.map(this::toName)))),
			equalToNames(C3.class, C5.class, I3.class, C6.class, AC3.class, C7.class));
	}

	@Test
	public void testTypesAnnotatedInherited() {
		assertThat(reflections.get(SubTypes.of(TypesAnnotated.with(MAI1.class)).as(Class.class).filter(withAnnotation(MAI1.class))),
			equalTo(AI1.class));

		AC2 ac2 = new AC2() {
			public String value() { return "ac2"; }
			public Class<? extends Annotation> annotationType() { return AC2.class; }
		};

		assertThat(reflections.get(SubTypes.of(TypesAnnotated.with(AC2.class)).as(Class.class).filter(withAnnotation(ac2))),
			equalTo(C3.class, I3.class, AC3.class));
	}

	@Test
	public void testMethodsAnnotated() throws NoSuchMethodException {
		assertThat(reflections.get(MethodsAnnotated.with(AM1.class)),
			equalToNames(C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m1", int.class, String[].class),
				C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
				C4.class.getDeclaredMethod("m3")));

		AM1 am1 = new AM1() {
			public String value() {
				return "1";
			}
			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat(reflections.get(MethodsAnnotated.with(AM1.class)
				.as(Method.class, loaders).filter(withAnnotation(am1))),
			equalTo(C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m1", int.class, String[].class),
				C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));
	}

	@Test
	public void testConstructorsAnnotated() throws NoSuchMethodException {
		assertThat(reflections.get(ConstructorsAnnotated.with(AM1.class)),
			equalToNames(C4.class.getDeclaredConstructor(String.class)));

		AM1 am1 = new AM1() {
			public String value() {
				return "1";
			}
			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat(reflections.get(ConstructorsAnnotated.with(AM1.class)
				.as(Constructor.class, loaders).filter(withAnnotation(am1))),
			equalTo(C4.class.getDeclaredConstructor(String.class)));
	}

	@Test
	public void testFieldsAnnotated() throws NoSuchFieldException {
		assertThat(reflections.get(FieldsAnnotated.with(AF1.class)),
			equalToNames(C4.class.getDeclaredField("f1"),
				C4.class.getDeclaredField("f2")));

		AF1 af1 = new AF1() {
			public String value() {
				return "2";
			}
			public Class<? extends Annotation> annotationType() {
				return AF1.class;
			}
		};

		assertThat(reflections.get(FieldsAnnotated.with(AF1.class)
				.as(Field.class, loaders).filter(withAnnotation(af1))),
			equalTo(C4.class.getDeclaredField("f2")));
	}

	@Test
	public void testMethodParameter() throws NoSuchMethodException {
		assertThat(reflections.get(MethodsParameter.with(String.class)),
			equalToNames(C4.class.getDeclaredMethod("m4", String.class)));

		assertThat(reflections.get(MethodsSignature.with()),
			equalToNames(C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m3"),
				AC2.class.getMethod("value"),
				AF1.class.getMethod("value"),
				AM1.class.getMethod("value")));

		assertThat(reflections.get(MethodsSignature.with(int[][].class, String[][].class)),
			equalToNames(C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

		assertThat(reflections.get(MethodsReturn.of(int.class)),
			equalToNames(C4.class.getDeclaredMethod("add", int.class, int.class)));

		assertThat(reflections.get(MethodsReturn.of(String.class)),
			equalToNames(C4.class.getDeclaredMethod("m3"),
				C4.class.getDeclaredMethod("m4", String.class),
				AC2.class.getMethod("value"),
				AF1.class.getMethod("value"),
				AM1.class.getMethod("value")));

		assertThat(reflections.get(MethodsReturn.of(void.class)),
			equalToNames(C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m1", int.class, String[].class),
				C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

		assertThat(reflections.get(MethodsParameter.with(AM1.class)),
			equalToNames(C4.class.getDeclaredMethod("m4", String.class)));

		AM1 am1 = new AM1() {
			public String value() {
				return "2";
			}
			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat(reflections.get(MethodsParameter.with(AM1.class).as(Method.class).filter(withAnyParameterAnnotation(am1))),
			equalTo(C4.class.getDeclaredMethod("m4", String.class)));
	}

	@Test
	public void testConstructorParameter() throws NoSuchMethodException {
		assertThat(reflections.get(ConstructorsParameter.with(String.class)),
			equalToNames(C4.class.getDeclaredConstructor(String.class)));

		assertThat(reflections.get(ConstructorsSignature.with()),
			equalToNames(C1.class.getDeclaredConstructor(),
				C2.class.getDeclaredConstructor(),
				C3.class.getDeclaredConstructor(),
				C4.class.getDeclaredConstructor(),
				C5.class.getDeclaredConstructor(),
				C6.class.getDeclaredConstructor(),
				C7.class.getDeclaredConstructor()));

		assertThat(reflections.get(ConstructorsParameter.with(AM1.class)),
			equalToNames(C4.class.getDeclaredConstructor(String.class)));

		AM1 am1 = new AM1() {
			public String value() {
				return "1";
			}

			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat(reflections.get(ConstructorsParameter.with(AM1.class)
				.as(Constructor.class, loaders)
				.filter(withAnnotation(am1))),
			equalTo(C4.class.getDeclaredConstructor(String.class)));
	}

	@Test
	public void testResourcesScanner() {
		assertThat(reflections.get(Resources.with(".*resource1-reflections\\.xml")),
			equalTo("META-INF/reflections/resource1-reflections.xml"));

		assertThat(reflections.get(Resources.with(".*")),
			equalTo("META-INF/reflections/testModel-reflections.xml",
				"META-INF/reflections/saved-testModel-reflections.xml",
				"META-INF/reflections/resource1-reflections.xml",
				"META-INF/reflections/inner/resource2-reflections.xml"));
	}

	@Test
	public void testGetAll() {
		assertThat(reflections.getAll(SubTypes),
			equalTo("java.lang.Object", "java.lang.annotation.Annotation",
				"org.reflections.TestModel$MAI1", "org.reflections.TestModel$AI1", "org.reflections.TestModel$AI2",
				"org.reflections.TestModel$I1", "org.reflections.TestModel$I2", "org.reflections.TestModel$I3",
				"org.reflections.TestModel$AF1", "org.reflections.TestModel$AM1",
				"org.reflections.TestModel$AC1", "org.reflections.TestModel$AC1n", "org.reflections.TestModel$AC2", "org.reflections.TestModel$AC3",
				"org.reflections.TestModel$C1", "org.reflections.TestModel$C2", "org.reflections.TestModel$C3", "org.reflections.TestModel$C4",
				"org.reflections.TestModel$C5", "org.reflections.TestModel$C6", "org.reflections.TestModel$C7"));
	}

	//
	@SafeVarargs
	public static <T> Matcher<Collection<T>> equalTo(T... operand) {
		return IsEqual.equalTo(new LinkedHashSet<>(Arrays.asList(operand)));
	}

	@SafeVarargs
	public final <T extends AnnotatedElement> Matcher<Collection<String>> equalToNames(T... operand) {
		return IsEqual.equalTo(new LinkedHashSet<>(toNames(operand)));
	}
}
