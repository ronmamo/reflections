package org.reflections.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;

abstract class AbstractIndexedListIterator<E>
		extends UnmodifiableListIterator<E>
		implements ListIterator<E> {
	
	private final int size;
	private int position;

	protected abstract E get(int index);

	protected AbstractIndexedListIterator(int size) {
		this(size, 0);
	}

	protected AbstractIndexedListIterator(int size, int position) {
		this.size = size;
		this.position = position;
	}

	@Override
	public final boolean hasNext() {
		return position < size;
	}

	@Override
	public final E next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return get(position++);
	}

	@Override
	public final int nextIndex() {
		return position;
	}

	@Override
	public final boolean hasPrevious() {
		return position > 0;
	}

	@Override
	public final E previous() {
		if (!hasPrevious()) {
			throw new NoSuchElementException();
		}
		return get(--position);
	}

	@Override
	public final int previousIndex() {
		return position - 1;
	}
}
