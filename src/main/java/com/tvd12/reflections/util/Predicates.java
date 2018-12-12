package com.tvd12.reflections.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Predicate;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class Predicates {

	public static Predicate ALWAYS_TRUE_PREDICATE = new AlwaysTruePredicate<>();
	
	private Predicates() {
	}
	
	public static Predicate alwaysTrue() {
		return ALWAYS_TRUE_PREDICATE;
	}

	public static Predicate and(Predicate[] predicates) {
		return new Predicate() {

			@Override
			public boolean test(Object t) {
				for (Predicate predicate : predicates) {
					if (!predicate.test(t))
						return false;
				}
				return true;
			}
		};
	}

	public static <T> Predicate<T> in(Collection<? extends T> target) {
		return new InPredicate<T>(target);
	}

	public static <T> Predicate<T> not(Predicate<T> predicate) {
		return new NotPredicate<T>(predicate);
	}

	private static class InPredicate<T> implements Predicate<T>, Serializable {
		private static final long serialVersionUID = -8049890389593544847L;

		private final Collection<?> target;

		private InPredicate(Collection<?> target) {
			this.target = target;
		}

		@Override
		public boolean test(T t) {
			try {
				return target.contains(t);
			} catch (NullPointerException | ClassCastException e) {
				return false;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof InPredicate) {
				InPredicate<?> that = (InPredicate<?>) obj;
				return target.equals(that.target);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return target.hashCode();
		}

		@Override
		public String toString() {
			return "Predicates.in(" + target + ")";
		}
	}

	private static class NotPredicate<T> implements Predicate<T>, Serializable {
		private static final long serialVersionUID = 8132922606124731479L;
		
		private final Predicate<T> predicate;

		private NotPredicate(Predicate<T> predicate) {
			this.predicate = predicate;
		}

		@Override
		public boolean test(T t) {
			return !predicate.test(t);
		}

		@Override
		public int hashCode() {
			return ~predicate.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NotPredicate) {
				NotPredicate<?> that = (NotPredicate<?>) obj;
				return predicate.equals(that.predicate);
			}
			return false;
		}

		@Override
		public String toString() {
			return "Predicates.not(" + predicate + ")";
		}
	}
	
	private static class AlwaysTruePredicate<T> implements Predicate<T> {

		@Override
		public boolean test(T t) {
			return true;
		}
		
	}

}
