package com.tvd12.reflections.exception.testing;

import com.tvd12.reflections.Reflections;

public class MainTest {

	public static void main(String[] args) {
		Reflections reflections = new Reflections("com");
		System.out.println(Reflections.log);
		reflections.getSubTypesOf(BaseClass.class);
	}
	
}
