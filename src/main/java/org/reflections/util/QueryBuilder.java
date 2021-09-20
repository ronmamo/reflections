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

	default QueryFunction<Store, String> get(String key) {
		return store -> new LinkedHashSet<>(store.getOrDefault(index(), Collections.emptyMap()).getOrDefault(key, Collections.emptySet()));
	}

	// get/getAll/getAllIncluding
	default QueryFunction<Store, String> get(AnnotatedElement element) { return get(toName(element)); }
	default QueryFunction<Store, String> get(Collection<String> keys) { return keys.stream().map(this::get).reduce(QueryFunction::add).get(); }
	default QueryFunction<Store, String> getAll(Collection<String> keys) { return QueryFunction.set(keys).getAll(this::get); }
	default QueryFunction<Store, String> getAllIncluding(String key) { return QueryFunction.single(key).add(QueryFunction.single(key).getAll(this::get)); }
	default QueryFunction<Store, String> getAllIncluding(Collection<String> keys) { return QueryFunction.set(keys).add(QueryFunction.set(keys).getAll(this::get)); }

	// of/with syntactics
	default QueryFunction<Store, String> of(Collection<String> keys) { return getAll(keys); }
	default QueryFunction<Store, String> of(String key) { return getAll(Collections.singletonList(key)); }
	default QueryFunction<Store, String> of(AnnotatedElement... elements) { return getAll(toNames(elements)); }
	default QueryFunction<Store, String> of(Set<? extends AnnotatedElement> elements) { return getAll(toNames(elements)); }

	default QueryFunction<Store, String> with(Collection<String> keys) { return of(keys); }
	default QueryFunction<Store, String> with(String key) { return of(key); }
	default QueryFunction<Store, String> with(Class<?>... keys) { return of(keys); }
	default QueryFunction<Store, String> with(Set<? extends AnnotatedElement> keys) { return of(keys); }

	// compose QueryFunction
	default <T> QueryFunction<Store, T> of(QueryFunction queryFunction) {
		return queryFunction.add(queryFunction.getAll((Function<String, QueryFunction<Store, String>>) this::get));
	}
	default <T> QueryFunction<Store, String> with(QueryFunction queryFunction) {
		return store -> QueryFunction.set(queryFunction.apply(store)).getAll((Function<String, QueryFunction<Store, String>>) this::get).apply(store);
	}
}
