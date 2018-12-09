package org.reflections.util;

import java.util.Iterator;

public abstract class FluentIterable<E> implements Iterable<E> {

	@Override
	public Iterator<E> iterator() {
		return null;
	}

	public static <T> FluentIterable<T> concat(final Iterable<? extends Iterable<? extends T>> inputs) {
		return new FluentIterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return Iterators.concat(
					Iterators.transform(inputs.iterator(), Iterables.toIterator())
				);
			}
		};
	}

}
