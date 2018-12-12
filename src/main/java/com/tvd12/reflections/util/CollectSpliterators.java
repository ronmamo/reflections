package com.tvd12.reflections.util;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CollectSpliterators {
	
	private CollectSpliterators() {
	}

	public static <T> Spliterator<T> filter(
			Spliterator<T> fromSpliterator, 
			Predicate<T> predicate) {
		class Splitr implements Spliterator<T>, Consumer<T> {
		      T holder = null;
		 
		      @Override 
		      public void accept(T t) {
		        this.holder = t;
		      } 
		 
		      @Override 
		      public boolean tryAdvance(Consumer<? super T> action) {
		        while (fromSpliterator.tryAdvance(this)) {
		          try { 
		            if (predicate.test(holder)) {
		              action.accept(holder);
		              return true; 
		            } 
		          } finally { 
		            holder = null;
		          } 
		        } 
		        return false; 
		      } 
		 
		      @Override 
		      public Spliterator<T> trySplit() {
		        Spliterator<T> fromSplit = fromSpliterator.trySplit();
		        return (fromSplit == null) ? null : filter(fromSplit, predicate);
		      } 
		 
		      @Override 
		      public long estimateSize() { 
		        return fromSpliterator.estimateSize() / 2;
		      } 
		 
		      @Override 
		      public Comparator<? super T> getComparator() {
		        return fromSpliterator.getComparator();
		      } 
		 
		      @Override 
		      public int characteristics() { 
		        return fromSpliterator.characteristics()
		            & (Spliterator.DISTINCT
		                | Spliterator.NONNULL
		                | Spliterator.ORDERED
		                | Spliterator.SORTED);
		      } 
		    } 
		    return new Splitr(); 
	}

}
