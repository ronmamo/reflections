package com.tvd12.reflections.util;

import java.util.Iterator;

abstract class TransformedIterator<F, T> implements Iterator<T> {
	final Iterator<? extends F> backingIterator;

	TransformedIterator(Iterator<? extends F> backingIterator) {
		this.backingIterator = backingIterator;
	}

	abstract T transform(F from);

	@Override
	public final boolean hasNext() {
		return backingIterator.hasNext();
	}

	@Override
	public final T next() {
		return transform(backingIterator.next());
	}

	@Override
	public final void remove() {
		backingIterator.remove();
	}
}