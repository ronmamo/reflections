package com.tvd12.reflections.util;

import java.util.ListIterator;

public abstract class UnmodifiableListIterator<E>
		extends UnmodifiableIterator<E>
		implements ListIterator<E> {

	public final void add(E e) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final void set(E e) {
		throw new UnsupportedOperationException();
	}
	
}
