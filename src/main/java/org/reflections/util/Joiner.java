package org.reflections.util;

import java.util.Collection;

public class Joiner {

	private final String separator;
	
	public Joiner(String separator) {
		this.separator = separator;
	}
	
	public static Joiner on(String string) {
		return new Joiner(string);
	}

	public String join(Collection<? extends Object> objects) {
		int size = objects.size();
		StringBuilder builder = new StringBuilder();
		int index = 0;
		for(Object item : objects) {
			builder.append(item);
			if((index ++) < size - 1)
				builder.append(separator);
		}
		return builder.toString();
	}
	
}
