package com.tvd12.reflections.testing;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TsIterators {

	public static boolean contains(Iterator<?> iterator, Object element) {
		if (element == null) {
			while (iterator.hasNext()) {
				if (iterator.next() == null) {
					return true;
				}
			}
		} else {
			while (iterator.hasNext()) {
				if (element.equals(iterator.next())) {
					return true;
				}
			}
		}
		return false;
	}

	public static <T> Iterator<T> limit(final Iterator<T> iterator, final int limitSize) {
		return new Iterator<T>() {
			private int count;

			@Override
			public boolean hasNext() {
				return count < limitSize && iterator.hasNext();
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				count++;
				return iterator.next();
			}

			@Override
			public void remove() {
				iterator.remove();
			}
		};
	}

}
