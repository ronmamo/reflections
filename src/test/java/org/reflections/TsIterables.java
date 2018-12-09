package org.reflections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reflections.util.FluentIterable;
import org.reflections.util.Iterators;

public class TsIterables {

	public static <F, T> Iterable<T> transform(final Iterable<F> fromIterable,
	        final Function<? super F, ? extends T> function) {
		return new FluentIterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return Iterators.transform(fromIterable.iterator(), function);
			}

			@Override
			public void forEach(Consumer<? super T> action) {
				fromIterable.forEach((F f) -> action.accept(function.apply(f)));
			}

			@Override
			public Spliterator<T> spliterator() {
				return TsCollectSpliterators.map(fromIterable.spliterator(), function);
			}
		};
	}

	public static boolean contains(Iterable<?> iterable, Object element) {
		if (iterable instanceof Collection) {
			Collection<?> collection = (Collection<?>) iterable;
			return Collections2.safeContains(collection, element);
		}
		return TsIterators.contains(iterable.iterator(), element);
	}

	public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limitSize) {
		return new FluentIterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return TsIterators.limit(iterable.iterator(), limitSize);
			}

			@Override
			public Spliterator<T> spliterator() {
				int index = 0;
				List<T> list = new ArrayList<>();
				for(T t : iterable) {
					if(index < limitSize)
						list.add(t);
				}
				return list.spliterator();
			}
		};
	}

}
