package org.reflections.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({ "unchecked", "rawtypes" })
public final class Iterables {

	private Iterables() {
	}

	public static boolean any(Set set, Predicate predicate) {
		for (Object item : set) {
			if (predicate.test(item))
				return true;
		}
		return false;
	}

	public static <T> Iterable<T> filter(final Iterable<T> unfiltered, final Predicate<T> retainIfTrue) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return Iterators.filter(unfiltered.iterator(), retainIfTrue);
			}

			@Override
			public void forEach(Consumer<? super T> action) {
				unfiltered.forEach((T a) -> {
					if (retainIfTrue.test(a)) {
						action.accept(a);
					}
				});
			}

			@Override
			public Spliterator<T> spliterator() {
				return CollectSpliterators.filter(unfiltered.spliterator(), retainIfTrue);
			}
		};
	}

	public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
		return FluentIterable.concat(a, b);
	}

	public static <T> Iterable<T> concat(Iterable<? extends T>... inputs) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return Iterators.concat(new AbstractIndexedListIterator<Iterator<? extends T>>(inputs.length) {
					@Override
					public Iterator<? extends T> get(int i) {
						return inputs[i].iterator();
					}
				});
			}
		};
	}

	public static <T> Iterable<T> concat(Iterable<? extends Iterable<? extends T>> inputs) {
		return FluentIterable.concat(inputs);
	}

	public static boolean isEmpty(Iterable<?> iterable) {
		if (iterable instanceof Collection) {
			return ((Collection<?>) iterable).isEmpty();
		}
		return !iterable.iterator().hasNext();
	}

	public static <T> T getOnlyElement(Iterable<T> iterable) {
		return Iterators.getOnlyElement(iterable.iterator());
	}

	static <T> Function<Iterable<? extends T>, Iterator<? extends T>> toIterator() {
		return new Function<Iterable<? extends T>, Iterator<? extends T>>() {
			@Override
			public Iterator<? extends T> apply(Iterable<? extends T> iterable) {
				return iterable.iterator();
			}
		};
	}

}
