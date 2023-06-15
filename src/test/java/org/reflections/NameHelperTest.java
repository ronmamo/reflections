package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.util.NameHelper;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"unchecked"})
public class NameHelperTest implements NameHelper {

	@Test
	public void testClass() {
		assertToFor(String.class, this::toName, this::forClass);
		assertToFor(String[].class, this::toName, this::forClass);
		assertToFor(boolean.class, this::toName, this::forClass);
		assertNull(forClass("no.exist"));
	}

	@Test
	public void testConstructor() throws NoSuchMethodException {
		assertToFor(String.class.getDeclaredConstructor(), this::toName, this::forConstructor);
		assertToFor(String.class.getDeclaredConstructor(String.class), this::toName, this::forConstructor);
	}

	@Test
	public void testMethod() throws NoSuchMethodException {
		assertToFor(String.class.getDeclaredMethod("length"), this::toName, this::forMethod);
		assertToFor(String.class.getDeclaredMethod("charAt", int.class), this::toName, this::forMethod);
	}

	@Test
	public void testField() throws NoSuchFieldException {
		assertToFor(String.class.getDeclaredField("value"), this::toName, this::forField);
	}

	@Test
	public void testToForNames() throws NoSuchFieldException, NoSuchMethodException {
		Class<String> CLASS = String.class;
		Constructor<String> CONST = CLASS.getDeclaredConstructor();
		Method METHOD = CLASS.getDeclaredMethod("length");
		Field FIELD = CLASS.getDeclaredField("value");

		Set<AnnotatedElement> elements = set(CLASS, CONST, METHOD, FIELD);
		Collection<String> names = toNames(elements);

		assertEquals(set(CLASS), forNames(names));
		assertEquals(set(CLASS), forNames(names, Class.class));
		assertEquals(set(CONST), forNames(names, Constructor.class));
		assertEquals(set(METHOD), forNames(names, Method.class));
		assertEquals(set(FIELD), forNames(names, Field.class));
		assertEquals(set(CONST, METHOD, FIELD), forNames(names, Member.class));
	}

	@Test
	// cherry-picked from PR #290 by @ziqin
	public void testMethodExt() throws NoSuchMethodException {
		// synthetic method for lambda expression
		Member lambda = forMember("org.reflections.UsageTestModel$C2.lambda$useLambda$0(org.reflections.UsageTestModel$C2)");
		assertEquals(lambda.getName(), "lambda$useLambda$0");
		assertEquals(lambda.getDeclaringClass(), UsageTestModel.C2.class);
		assertTrue(lambda.isSynthetic());

		// method of anonymous inner class
		Member anonymous = forMember("org.reflections.UsageTestModel$C2$1.applyAsDouble(org.reflections.UsageTestModel$C2)");
		assertEquals(anonymous.getName(), "applyAsDouble");
		assertEquals(anonymous.getDeclaringClass(), forClass(UsageTestModel.C2.class.getName() + "$1"));
	}

	<T> void assertToFor(T type, Function<T, String> toName, Function<String, T> forName) {
		assertEquals(forName.apply(toName.apply(type)), type);
	}

	private <T> Set<T> set(T... ts) {
		return new HashSet<>(Arrays.asList(ts));
	}
}