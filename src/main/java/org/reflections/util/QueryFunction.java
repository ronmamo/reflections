package org.reflections.util;

import org.reflections.Store;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * sam function for store query {@link #apply(C)} -> {@code Set<T>}
 * <pre>{@code QueryFunction<T> query = ctx -> ctx.get(key) }</pre>
 * <p>supports functional composition {@link #filter(Predicate)}, {@link #map(Function)}, {@link #flatMap(Function)}, ...
 */
public interface QueryFunction<C, T> extends Function<C, Set<T>>, NameHelper {
	/* @inherited */
	Set<T> apply(C ctx);

	static <C, T> QueryFunction<Store, T> empty() { return ctx -> Collections.emptySet(); }
	static <C, T> QueryFunction<Store, T> single(T element) { return ctx -> Collections.singleton(element); }
	static <C, T> QueryFunction<Store, T> set(Collection<T> elements) { return ctx -> new LinkedHashSet<>(elements); }

	/** filter by predicate <pre>{@code SubTypes.of(type).filter(withPrefix("org"))}</pre>*/
	default QueryFunction<C, T> filter(Predicate<? super T> predicate) {
		return ctx -> apply(ctx).stream().filter(predicate).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** map by function <pre>{@code TypesAnnotated.with(annotation).asClass().map(Annotation::annotationType)}</pre>*/
	default <R> QueryFunction<C, R> map(Function<? super T, ? extends R> function) {
		return ctx -> apply(ctx).stream().map(function).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** flatmap by function <pre>{@code QueryFunction<Method> methods = SubTypes.of(type).asClass().flatMap(Methods::of)}</pre> */
	default <R> QueryFunction<C, R> flatMap(Function<T, ? extends Function<C, Set<R>>> function) {
		return ctx -> apply(ctx).stream().flatMap(t -> function.apply(t).apply(ctx).stream()).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** transitively get all by {@code builder} <pre>{@code SuperTypes.of(type).getAll(Annotations::get)}</pre>*/
	default QueryFunction<C, T> getAll(Function<T, QueryFunction<C, T>> builder) {
		return getAll(builder, t -> t);
	}

	/** transitively get all by {@code builder} <pre>{@code SuperTypes.of(type).getAll(Annotations::get)}</pre>*/
	default <R> QueryFunction<C, R> getAll(Function<T, QueryFunction<C, R>> builder, Function<R, T> traverse) {
		return ctx -> {
			List<T> workKeys = new ArrayList<>(apply(ctx));
			Set<R> result = new LinkedHashSet<>();
			for (int i = 0; i < workKeys.size(); i++) {
				T key = workKeys.get(i);
				Set<R> apply = builder.apply(key).apply(ctx);
				for (R r : apply) if (result.add(r)) workKeys.add(traverse.apply(r));
			}
			return result;
		};
	}

	/** concat elements from function <pre>{@code Annotations.of(method).add(Annotations.of(type))}</pre>*/
	default <R> QueryFunction<C, T> add(QueryFunction<C, T> function) {
		return ctx -> Stream.of(apply(ctx), function.apply(ctx))
			.flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** convert to given @{code type}, uses {@link NameHelper#forName(java.lang.String, java.lang.Class, java.lang.ClassLoader...)}
	 * <pre>{@code Methods.of(type).as(Method.class)}</pre>*/
	default <R> QueryFunction<C, R> as(Class<? extends R> type, ClassLoader... loaders) {
		return ctx -> {
			Set<T> apply = apply(ctx);
			//noinspection unchecked
			return (Set<R>) apply.stream().findFirst().map(first ->
				type.isAssignableFrom(first.getClass()) ? apply :
				first instanceof String ? ((Set<R>) forNames((Collection<String>) apply, type, loaders)) :
				first instanceof AnnotatedElement ? ((Set<R>) forNames(toNames((Collection<AnnotatedElement>) apply), type, loaders)) :
					apply.stream().map(t -> (R) t).collect(Collectors.toCollection(LinkedHashSet::new))
			).orElse(apply);
		};
	}

	/** convert elements to {@code Class} using {@link NameHelper#forName(java.lang.String, java.lang.Class, java.lang.ClassLoader...)}
	 * <pre>{@code SubTypes.of(type).asClass()}</pre> */
	default <R> QueryFunction<C, Class<?>> asClass(ClassLoader... loaders) {
		// noinspection unchecked
		return ctx -> ((Set<Class<?>>) forNames((Set) apply(ctx), Class.class, loaders));
	}

	/** convert elements to String using {@link NameHelper#toName(AnnotatedElement)}*/
	default QueryFunction<C, String> asString() {
		return ctx -> new LinkedHashSet<>(toNames((AnnotatedElement) apply(ctx)));
	}

	/** cast elements as {@code <R>} unsafe */
	default <R> QueryFunction<C, Class<? extends R>> as() {
		return ctx -> new LinkedHashSet<>((Set<? extends Class<R>>) apply(ctx));
	}
}
