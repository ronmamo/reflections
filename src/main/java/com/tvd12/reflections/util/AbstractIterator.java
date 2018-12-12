package com.tvd12.reflections.util;

import java.util.NoSuchElementException;

public abstract class AbstractIterator<T> extends UnmodifiableIterator<T> {
	private State state = State.NOT_READY;

	/** Constructor for use by subclasses. */
	protected AbstractIterator() {
	}

	private enum State {
		READY,
		NOT_READY,
		DONE,
		FAILED,
	}

	private T next;

	protected abstract T computeNext();

	protected final T endOfData() {
		state = State.DONE;
		return null;
	}

	@Override
	public final boolean hasNext() {
		if(state == State.FAILED)
			throw new IllegalStateException();
		switch (state) {
		case DONE:
			return false;
		case READY:
			return true;
		default:
		}
		return tryToComputeNext();
	}

	private boolean tryToComputeNext() {
		state = State.FAILED;
		next = computeNext();
		if (state != State.DONE) {
			state = State.READY;
			return true;
		}
		return false;
	}

	@Override
	public final T next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		state = State.NOT_READY;
		T result = next;
		next = null;
		return result;
	}

	public final T peek() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return next;
	}
}