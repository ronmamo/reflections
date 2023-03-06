package org.reflections.util;

import org.reflections.ReflectionUtils;
import org.reflections.Store;

import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * query builder for {@link QueryFunction}
 * <pre>{@code UtilQueryBuilder<Annotation> builder =
 *   element -> store -> element.getDeclaredAnnotations()} </pre>
 */
public interface UtilQueryBuilder<F, E> {
	/** get direct values of given element */
	QueryFunction<Store, E> get(F element);

	/** get transitive values of given element */
	default QueryFunction<Store, E> of(final F element) {
		return of(ReflectionUtils.<Class<?>>extendType().get((AnnotatedElement) element));
	}

	/** get transitive value of given element filtered by predicate */
	default QueryFunction<Store, E> of(final F element, Predicate<? super E> predicate) {
		return of(element).filter(predicate);
	}

	/** compose given function */
	default <T> QueryFunction<Store, E> of(QueryFunction<Store, T> function) {
		return store -> function.apply(store).stream()
			.flatMap(t -> get((F) t).apply(store).stream()).collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
