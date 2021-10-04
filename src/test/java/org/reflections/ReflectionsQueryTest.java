package org.reflections;

import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
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

	public ReflectionsQueryTest() {
		reflections = new Reflections(
			new ConfigurationBuilder()
				.forPackage("org.reflections")
				.filterInputsBy(new FilterBuilder()
					.includePattern("org\\.reflections\\.TestModel\\$.*")
					.or(s -> s.endsWith(".xml")))
				.setScanners(Scanners.values()));
	}

	@Test
	public void testSubTypes() {
		assertThat("direct subtypes of interface",
			reflections.get(SubTypes.get(I1.class)),
			equalToNames(I2.class));

		assertThat("direct subtypes of class",
			reflections.get(SubTypes.get(C1.class).asClass()),
			equalTo(C2.class, C3.class));

		assertThat("transitive subtypes of interface",
			reflections.get(SubTypes.of(I1.class)),
			equalToNames(I2.class, C1.class, C2.class, C3.class, C5.class));

		assertThat("transitive subtypes of class",
			reflections.get(SubTypes.of(C1.class).asClass()),
			equalTo(C2.class, C3.class, C5.class));
	}

	@Test
	public void testTypesAnnotated() {
		assertThat("direct types annotated with meta annotation",
			reflections.get(TypesAnnotated.get(MAI1.class).asClass()),
			equalTo(AI1.class));

		assertThat("transitive types annotated with meta annotation",
			reflections.get(TypesAnnotated.of(MAI1.class).asClass()),
			equalTo(AI1.class, I1.class));

		assertThat("transitive subtypes of types annotated with meta annotation, including",
			reflections.get(SubTypes.of(TypesAnnotated.with(MAI1.class)).asClass()),
			equalTo(AI1.class, I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));

		assertThat("direct types annotated with annotation",
			reflections.get(TypesAnnotated.get(AI1.class)),
			equalToNames(I1.class));

		assertThat("transitive types annotated with annotation",
			reflections.get(TypesAnnotated.of(AI1.class)),
			equalToNames(I1.class));

		assertThat("transitive subtypes of types annotated with annotation",
			reflections.get(SubTypes.of(TypesAnnotated.with(AI1.class))),
			equalToNames(I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));
	}

	@Test
	public void testTypesAnnotatedWithMemberMatching() {
		assertThat("direct types annotated with annotation",
			reflections.get(TypesAnnotated.get(AC2.class).asClass()),
			equalTo(C2.class, C3.class, I3.class, AC3.class));

		assertThat("transitive types annotated with annotation",
			reflections.get(TypesAnnotated.with(AC2.class).asClass()),
			equalTo(C2.class, C3.class, I3.class, AC3.class, C7.class));

		assertThat("transitive subtypes of types annotated with annotation",
			reflections.get(SubTypes.of(TypesAnnotated.with(AC2.class)).asClass()),
			equalTo(C2.class, C3.class, I3.class, AC3.class, C7.class, C5.class, C6.class));

		AC2 ac2 = new AC2() {
			public String value() { return "ac2"; }
			public Class<? extends Annotation> annotationType() { return AC2.class; }
		};

		assertThat("transitive types annotated with annotation filter by member matching",
			reflections.get(TypesAnnotated.with(AC2.class).asClass().filter(withAnnotation(ac2))),
			equalTo(C3.class, I3.class, AC3.class));

		assertThat("transitive subtypes of types annotated with annotation filter by member matching",
			reflections.get(SubTypes.of(TypesAnnotated.with(AC2.class).filter(a -> withAnnotation(ac2).test(forClass(a))))),
			equalToNames(C3.class, I3.class, AC3.class, C5.class, C6.class));
	}

	@Test
	public void testMethodsAnnotated() throws NoSuchMethodException {
		assertThat("methods annotated with annotation",
			reflections.get(MethodsAnnotated.with(AM1.class)),
			equalToNames(
				C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m1", int.class, String[].class),
				C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
				C4.class.getDeclaredMethod("m3")));

		AM1 am11 = new AM1() {
			public String value() {
				return "1";
			}
			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat("methods annotated with annotation filter by member matching",
			reflections.get(MethodsAnnotated.with(AM1.class).as(Method.class).filter(withAnnotation(am11))),
			equalTo(
				C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m1", int.class, String[].class),
				C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));
	}

	@Test
	public void testConstructorsAnnotated() throws NoSuchMethodException {
		assertThat("constructors annotated with annotation",
			reflections.get(ConstructorsAnnotated.with(AM1.class)),
			equalToNames(C4.class.getDeclaredConstructor(String.class)));

		AM1 am12 = new AM1() {
			public String value() {
				return "2";
			}
			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat("constructors annotated with annotation filter by member matching",
			reflections.get(ConstructorsAnnotated.with(AM1.class)
				.as(Constructor.class).filter(withAnnotation(am12))),
			equalTo());
	}

	@Test
	public void testFieldsAnnotated() throws NoSuchFieldException {
		assertThat("fields annotated with annotation",
			reflections.get(FieldsAnnotated.with(AF1.class)),
			equalToNames(
				C4.class.getDeclaredField("f1"),
				C4.class.getDeclaredField("f2")));

		AF1 af12 = new AF1() {
			public String value() {
				return "2";
			}
			public Class<? extends Annotation> annotationType() {
				return AF1.class;
			}
		};

		assertThat("fields annotated with annotation filter by member matching",
			reflections.get(FieldsAnnotated.with(AF1.class)
				.as(Field.class).filter(withAnnotation(af12))),
			equalTo(C4.class.getDeclaredField("f2")));
	}

	@Test
	public void testMethods() throws NoSuchMethodException {
		assertThat("methods with any parameter",
			reflections.get(MethodsParameter.with(String.class)),
			equalToNames(C4.class.getDeclaredMethod("m4", String.class)));

		assertThat("methods with any parameter",
			reflections.get(MethodsParameter.with(int.class)),
			equalToNames(
				C4.class.getDeclaredMethod("m1", int.class, String[].class),
				C4.class.getDeclaredMethod("add", int.class, int.class)));

		assertThat("methods with signature single parameter",
			reflections.get(MethodsSignature.with(String.class)),
			equalToNames(C4.class.getDeclaredMethod("m4", String.class)));

		assertThat("methods with signature",
			reflections.get(MethodsSignature.with(int.class, String[].class)),
			equalToNames(C4.class.getDeclaredMethod("m1", int.class, String[].class)));

		assertThat("methods with signature no parameters",
			reflections.get(MethodsSignature.with()),
			equalToNames(
				C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m3"),
				AC2.class.getMethod("value"),
				AF1.class.getMethod("value"),
				AM1.class.getMethod("value")));

		assertThat("methods with return type",
			reflections.get(MethodsReturn.of(String.class)),
			equalToNames(
				C4.class.getDeclaredMethod("m3"),
				C4.class.getDeclaredMethod("m4", String.class),
				AC2.class.getMethod("value"),
				AF1.class.getMethod("value"),
				AM1.class.getMethod("value")));

		assertThat("methods with return type void",
			reflections.get(MethodsReturn.of(void.class)),
			equalToNames(C4.class.getDeclaredMethod("m1"),
				C4.class.getDeclaredMethod("m1", int.class, String[].class),
				C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

		assertThat("methods with parameter annotation",
			reflections.get(MethodsParameter.with(AM1.class)),
			equalToNames(C4.class.getDeclaredMethod("m4", String.class)));

		AM1 am1 = new AM1() {
			public String value() {
				return "2";
			}
			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat("methods with parameter annotation filter by member matching",
			reflections.get(MethodsParameter.with(AM1.class).as(Method.class).filter(withAnyParameterAnnotation(am1))),
			equalTo(C4.class.getDeclaredMethod("m4", String.class)));
	}

	@Test
	public void testConstructorParameter() throws NoSuchMethodException {
		assertThat("constructors with parameter",
			reflections.get(ConstructorsParameter.with(String.class)),
			equalToNames(C4.class.getDeclaredConstructor(String.class)));

		assertThat("constructors with signature no parameters",
			reflections.get(ConstructorsSignature.with()),
			equalToNames(
				C1.class.getDeclaredConstructor(),
				C2.class.getDeclaredConstructor(),
				C3.class.getDeclaredConstructor(),
				C4.class.getDeclaredConstructor(),
				C5.class.getDeclaredConstructor(),
				C6.class.getDeclaredConstructor(),
				C7.class.getDeclaredConstructor()));

		assertThat("constructors with parameter annotation",
			reflections.get(ConstructorsParameter.with(AM1.class)),
			equalToNames(C4.class.getDeclaredConstructor(String.class)));

		AM1 am1 = new AM1() {
			public String value() {
				return "1";
			}
			public Class<? extends Annotation> annotationType() {
				return AM1.class;
			}
		};

		assertThat("constructors with parameter annotation filter by member values",
			reflections.get(ConstructorsParameter.with(AM1.class)
				.as(Constructor.class)
				.filter(withAnnotation(am1))),
			equalTo(C4.class.getDeclaredConstructor(String.class)));
	}

	@Test
	public void testResourcesScanner() {
		assertThat("resources matching pattern",
			reflections.get(Resources.with(".*resource1-reflections\\.xml")),
			equalTo("META-INF/reflections/resource1-reflections.xml"));

		assertThat("resources matching pattern any",
			reflections.get(Resources.with(".*")),
			equalTo(
				"META-INF/reflections/testModel-reflections.xml",
				"META-INF/reflections/saved-testModel-reflections.xml",
				"META-INF/reflections/resource1-reflections.xml",
				"META-INF/reflections/inner/resource2-reflections.xml"));
	}

	@Test
	public void testGetAll() {
		reflections = new Reflections(
			new ConfigurationBuilder()
				.forPackage("org.reflections")
				.filterInputsBy(new FilterBuilder().includePattern("org\\.reflections\\.TestModel\\$.*"))
				.setScanners(Scanners.SubTypes.filterResultsBy(t -> true)));

		assertThat("all (sub) types",
			reflections.getAll(SubTypes),
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
