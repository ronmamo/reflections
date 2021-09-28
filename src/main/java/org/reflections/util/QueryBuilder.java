package org.reflections.util;

import org.reflections.Store;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/** builder for store query <pre>{@code QueryBuilder builder = element -> store -> Set<String>}</pre> */
public interface QueryBuilder extends NameHelper {

	default String index() { return getClass().getSimpleName(); }

	/** direct values indexed for {@code key} String
	 * <p>safely returns an empty {@code Set<String>} if {@code index/key} not found
	 * <p>this is the only function accessing the {@link Store} multimap */
	default QueryFunction<Store, String> get(String key) {
		return store -> new LinkedHashSet<>(store.getOrDefault(index(), Collections.emptyMap()).getOrDefault(key, Collections.emptySet()));
	}

	// get/getAll/getAllIncluding
	/** direct values indexed for {@code AnnotatedElement} */
	default QueryFunction<Store, String> get(AnnotatedElement element) { return get(toName(element)); }

	/** direct values indexed for {@code keys} String collection */
	default QueryFunction<Store, String> get(Collection<String> keys) { return keys.stream().map(this::get).reduce(QueryFunction::add).get(); }

	/** transitive values indexed for {@code keys} String collection, not including {@code keys} */
	default QueryFunction<Store, String> getAll(Collection<String> keys) { return QueryFunction.set(keys).getAll(this::get); }

	/** transitive values indexed for {@code key} String, including {@code key} */
	default QueryFunction<Store, String> getAllIncluding(String key) { return QueryFunction.single(key).add(QueryFunction.single(key).getAll(this::get)); }

	/** transitive values indexed for {@code keys} String collection, including {@code keys} */
	default QueryFunction<Store, String> getAllIncluding(Collection<String> keys) { return QueryFunction.set(keys).add(QueryFunction.set(keys).getAll(this::get)); }

	// of/with syntactics
	/** transitive values indexed for {@code keys} String collection, not including {@code keys} */
	default QueryFunction<Store, String> of(Collection<String> keys) { return getAll(keys); }

	/** transitive values indexed for {@code key} String, not including {@code key} */
	default QueryFunction<Store, String> of(String key) { return getAll(Collections.singletonList(key)); }

	/** transitive values indexed for {@code AnnotatedElement} varargs, not including */
	default QueryFunction<Store, String> of(AnnotatedElement... elements) { return getAll(toNames(elements)); }

	/** transitive values indexed for {@code AnnotatedElement} set, not including */
	default QueryFunction<Store, String> of(Set<? extends AnnotatedElement> elements) { return getAll(toNames(elements)); }

	/** transitive values indexed for {@code keys} String collection, not including {@code keys}. <p><i>same as {@link #of(Collection)}</i> */
	default QueryFunction<Store, String> with(Collection<String> keys) { return of(keys); }

	/** transitive values indexed for {@code key} String, not including {@code key}. <p><i>same as {@link #of(String)}</i> */
	default QueryFunction<Store, String> with(String key) { return of(key); }

	/** transitive values indexed for {@code AnnotatedElements} varargs, not including. <p><i>same as {@link #of(AnnotatedElement...)}</i> */
	default QueryFunction<Store, String> with(AnnotatedElement... keys) { return of(keys); }

	/** transitive values indexed for {@code AnnotatedElements} set, not including. <p><i>same as {@link #of(Set)}</i> */
	default QueryFunction<Store, String> with(Set<? extends AnnotatedElement> keys) { return of(keys); }

	// compose QueryFunction
	/** transitive {@link QueryFunction#getAll(java.util.function.Function)} values by applying this {@link #get(String)} on each {@code queryFunction} value, including */
	default <T> QueryFunction<Store, T> of(QueryFunction queryFunction) {
		return queryFunction.add(queryFunction.getAll((Function<String, QueryFunction<Store, String>>) this::get));
	}
}
