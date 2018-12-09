package org.reflections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reflections.util.Iterators;

public class Collections2 {

	public static <F, T> Collection<T> transform(Collection<F> fromCollection, Function<? super F, T> function) {
		return new TransformedCollection<>(fromCollection, function);
	}

	static boolean safeContains(Collection<?> collection, Object object) {
		try {
			return collection.contains(object);
		} catch (ClassCastException | NullPointerException e) {
			return false;
		}
	}

	static class TransformedCollection<F, T> extends AbstractCollection<T> {
		final Collection<F> fromCollection;
		final Function<? super F, ? extends T> function;

		TransformedCollection(Collection<F> fromCollection, Function<? super F, ? extends T> function) {
			this.fromCollection = fromCollection;
			this.function = function;
		}

		@Override
		public void clear() {
			fromCollection.clear();
		}

		@Override
		public boolean isEmpty() {
			return fromCollection.isEmpty();
		}

		@Override
		public Iterator<T> iterator() {
			return Iterators.transform(fromCollection.iterator(), function);
		}

		@Override
		public Spliterator<T> spliterator() {
			return TsCollectSpliterators.map(fromCollection.spliterator(), function);
		}

		@Override
		public void forEach(Consumer<? super T> action) {
			fromCollection.forEach((F f) -> action.accept(function.apply(f)));
		}

		@Override
		public boolean removeIf(Predicate<? super T> filter) {
			return fromCollection.removeIf(element -> filter.test(function.apply(element)));
		}

		@Override
		public int size() {
			return fromCollection.size();
		}
	}

}
