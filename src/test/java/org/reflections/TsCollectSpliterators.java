package org.reflections;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class TsCollectSpliterators {

	static <F, T> Spliterator<T> map(Spliterator<F> fromSpliterator, Function<? super F, ? extends T> function) {
		return new Spliterator<T>() {

			@Override
			public boolean tryAdvance(Consumer<? super T> action) {
				return fromSpliterator.tryAdvance(fromElement -> action.accept(function.apply(fromElement)));
			}

			@Override
			public void forEachRemaining(Consumer<? super T> action) {
				fromSpliterator.forEachRemaining(fromElement -> action.accept(function.apply(fromElement)));
			}

			@Override
			public Spliterator<T> trySplit() {
				Spliterator<F> fromSplit = fromSpliterator.trySplit();
				return (fromSplit != null) ? map(fromSplit, function) : null;
			}

			@Override
			public long estimateSize() {
				return fromSpliterator.estimateSize();
			}

			@Override
			public int characteristics() {
				return fromSpliterator.characteristics()
				        & ~(Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SORTED);
			}
		};
	}

}
