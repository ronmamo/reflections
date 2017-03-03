package org.reflections;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

/**
 * Unit test for bridged method
 */
public class BridgedMethodUsageTest {

	@Test
	public void testGetMethodUsageInAnonymousClass()
			throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		Reflections r = new Reflections("org.reflections", new MethodAnnotationsScanner(), new SubTypesScanner(),
				new MethodParameterScanner(), new TypeAnnotationsScanner(), new MemberUsageScanner());
		Class<?> target = Class.forName("org.reflections.scanners.MemberUsageScanner");
		Method[] methods = target.getDeclaredMethods();
		for (Method m : methods) {
			System.out.println(m.toGenericString());
		}
		Method outerMethod = target.getDeclaredMethod("put", String.class, Integer.TYPE, String.class);
		Set<Member> set = r.getMethodUsage(outerMethod);
		System.out.println("# caller: " + set.size());
		Assert.assertEquals(1, set.size());
		for (Member m : set) {
			System.out.println("caller: " + m.getDeclaringClass().getName() + "." + m.getName());
			Assert.assertEquals("org.reflections.scanners.MemberUsageScanner.scanMember",
					m.getDeclaringClass().getName() + "." + m.getName());
		}
	}
}
