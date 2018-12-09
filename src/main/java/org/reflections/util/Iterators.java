package org.reflections.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class Iterators {

	private Iterators() {
	}

	public static Iterator filter(Iterator unfiltered, Predicate retainIfTrue) {
		return new AbstractIterator() {
			@Override
			protected Object computeNext() {
				while (unfiltered.hasNext()) {
					Object element = unfiltered.next();
					if (retainIfTrue.test(element))
						return element;
				}
				return endOfData();
			}
		};
	}

	public static <T> Iterator<T> concat(Iterator<? extends Iterator<? extends T>> inputs) {
		return new ConcatenatedIterator<T>(inputs);
	}

	public static <T> T getOnlyElement(Iterator<T> iterator) {
		T first = iterator.next();
		if (!iterator.hasNext())
			return first;

		StringBuilder sb = new StringBuilder().append("expected one element but was: <").append(first);
		for (int i = 0; i < 4 && iterator.hasNext(); i++)
			sb.append(", ").append(iterator.next());
		if (iterator.hasNext())
			sb.append(", ...");
		sb.append('>');
		throw new IllegalArgumentException(sb.toString());
	}

	private static <T> UnmodifiableIterator<T> emptyIterator() {
		return emptyListIterator();
	}

	public static <T> UnmodifiableListIterator<T> emptyListIterator() {
		return (UnmodifiableListIterator<T>) ArrayItr.EMPTY;
	}

	private static final class ArrayItr<T> extends AbstractIndexedListIterator<T> {
		static final AbstractIndexedListIterator<Object> EMPTY = new ArrayItr<>(new Object[0], 0, 0, 0);

		private final T[] array;
		private final int offset;

		ArrayItr(T[] array, int offset, int length, int index) {
			super(length, index);
			this.array = array;
			this.offset = offset;
		}

		@Override
		protected T get(int index) {
			return array[offset + index];
		}
	}

	private static class ConcatenatedIterator<T> implements Iterator<T> {
		private Iterator<? extends T> toRemove;
		private Iterator<? extends T> iterator;
		private Iterator<? extends Iterator<? extends T>> topMetaIterator;
		private Deque<Iterator<? extends Iterator<? extends T>>> metaIterators;

		ConcatenatedIterator(Iterator<? extends Iterator<? extends T>> metaIterator) {
			iterator = emptyIterator();
			topMetaIterator = metaIterator;
		}

		private Iterator<? extends Iterator<? extends T>> getTopMetaIterator() {
			while (topMetaIterator == null || !topMetaIterator.hasNext()) {
				if (metaIterators != null && !metaIterators.isEmpty()) {
					topMetaIterator = metaIterators.removeFirst();
				} else {
					return null;
				}
			}
			return topMetaIterator;
		}

		@Override
		public boolean hasNext() {
			while (!iterator.hasNext()) {
				topMetaIterator = getTopMetaIterator();
				if (topMetaIterator == null) {
					return false;
				}

				iterator = topMetaIterator.next();

				if (iterator instanceof ConcatenatedIterator) {
					ConcatenatedIterator<T> topConcat = (ConcatenatedIterator<T>) iterator;
					iterator = topConcat.iterator;
					if (this.metaIterators == null) {
						this.metaIterators = new ArrayDeque<>();
					}
					this.metaIterators.addFirst(this.topMetaIterator);
					if (topConcat.metaIterators != null) {
						while (!topConcat.metaIterators.isEmpty()) {
							this.metaIterators.addFirst(topConcat.metaIterators.removeLast());
						}
					}
					this.topMetaIterator = topConcat.topMetaIterator;
				}
			}
			return true;
		}

		@Override
		public T next() {
			if (hasNext()) {
				toRemove = iterator;
				return iterator.next();
			} else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			toRemove.remove();
			toRemove = null;
		}
	}

	public static <F, T> Iterator<T> transform(final Iterator<F> fromIterator,
	        final Function<? super F, ? extends T> function) {
		return new TransformedIterator<F, T>(fromIterator) {
			@Override
			T transform(F from) {
				return function.apply(from);
			}
		};
	}

}
