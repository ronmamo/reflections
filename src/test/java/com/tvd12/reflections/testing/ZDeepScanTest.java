package com.tvd12.reflections.testing;

import java.util.Set;

import com.tvd12.reflections.Reflections;
import com.tvd12.test.annotation.ClientRequestListener;
import com.tvd12.test.annotation.ServerEventHandler;

public class ZDeepScanTest {
	
	public static void main(String[] args) {
		Reflections reflections = new Reflections(new Object[] {
				"com.tvd12.test", "com.tvd12.test1"});
		Set<Class<?>> requestClasses = reflections.getTypesAnnotatedWith(ClientRequestListener.class);
		Set<Class<?>> eventClasses = reflections.getTypesAnnotatedWith(ServerEventHandler.class);
		System.out.println(requestClasses);
		System.out.println(eventClasses);
	}
	
}
