package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.AnnotationMergeCollector;
import org.reflections.util.QueryFunction;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.reflections.ReflectionUtils.*;
import static org.reflections.ReflectionsQueryTest.equalTo;
import static org.reflections.TestModel.*;
import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.TypesAnnotated;

public class ReflectionUtilsQueryTest {

	@Test
	public void testTypes() throws NoSuchMethodException {
		assertThat(
			get(SuperTypes.of(C3.class)),
			equalTo(C1.class, I2.class, I1.class));

		assertThat(
			get(SuperTypes.of(C3.class)
				.filter(withAnnotation(AI1.class))),
			equalTo(I1.class));

		assertThat(
			get(Interfaces.get(C1.class)),
			equalTo(I2.class));

		assertThat(
			get(Interfaces.of(C3.class)),
			equalTo(I2.class, I1.class));

		assertThat(
			get(SuperClass.of(C5.class)),
			equalTo(C3.class, C1.class));

		assertThat(
			get(Annotations.of(TestModel.C3.class)
				.map(Annotation::annotationType)),
			equalTo(
				Retention.class, Target.class, Documented.class, Inherited.class,
				AC1.class, AC1n.class, AC2.class, AI1.class, AI2.class, MAI1.class));

		assertThat(
			get(AnnotationTypes.of(C3.class)
				.filter(a -> !a.getName().startsWith("java."))),
			equalTo(
				AC1.class, AC1n.class, AC2.class, AI1.class, AI2.class, MAI1.class));

		assertThat(
			get(Annotations.of(C4.class.getDeclaredMethod("m4", String.class))
				.map(Annotation::annotationType)),
			equalTo());
	}

	@Test
	public void testMembers() throws NoSuchMethodException, NoSuchFieldException {
		assertThat(
			get(Methods.of(C4.class, withName("m4"))),
			equalTo(C4.class.getDeclaredMethod("m4", String.class)));

		assertThat(
			get(Methods.of(C4.class, withParameters(String.class))),
			equalTo(C4.class.getDeclaredMethod("m4", String.class)));

		assertThat(
			get(Methods.of(C4.class)
				.filter(withPattern("public.*.void .*"))
				.map(Method::getName)),
			equalTo("m1"));

		assertThat(
			get(Methods.of(C4.class, withAnyParameterAnnotation(AM1.class))),
			equalTo(C4.class.getDeclaredMethod("m4", String.class)));

		assertThat(
			get(Methods.of(Class.class)
				.filter(withReturnType(Method.class).and(withPublic()))
				.map(Method::getName)),
			equalTo("getMethod", "getDeclaredMethod", "getEnclosingMethod"));

		assertThat(
			get(Fields.of(C4.class, withAnnotation(AF1.class))),
			equalTo(C4.class.getDeclaredField("f1"),
				C4.class.getDeclaredField("f2")));

		AF1 af12 = new AF1() {
			public String value() { return "2"; }
			public Class<? extends Annotation> annotationType() { return AF1.class; }
		};
		assertThat(
			get(Fields.of(C4.class)
				.filter(withAnnotation(af12))),
			equalTo(C4.class.getDeclaredField("f2")));

		assertThat(
			get(Fields.of(C4.class)
				.filter(withTypeAssignableTo(String.class))),
			equalTo(C4.class.getDeclaredField("f1"),
				C4.class.getDeclaredField("f2"),
				C4.class.getDeclaredField("f3")));

		assertThat(
			get(Constructors.of(C4.class)
				.filter(withParametersCount(0))),
			equalTo(C4.class.getDeclaredConstructor()));
	}

	@Test
	public void nestedQuery() {
		Set<Class<? extends Annotation>> annotations =
			get(Annotations.of(
					Methods.of(C4.class))
				.map(Annotation::annotationType)
				.filter(a -> !a.getName().startsWith("java."))
				.as());

		assertThat(annotations,
			equalTo(AM1.class));
	}

	@Test
	public void addQuery() {
		Set<Class<? extends Annotation>> annotations =
			get(AnnotationTypes.of(C1.class)
				.add(AnnotationTypes.of(C2.class)));

		assertThat(annotations,
			equalTo(
				Retention.class, Target.class, Documented.class, Inherited.class,
				AC1.class, AC2.class, AC1n.class, AI2.class, AI1.class, MAI1.class));
	}

	@Test
	public void singleQuery() {
		QueryFunction<Store, Class<?>> single =
			QueryFunction.single(CombinedTestModel.Impl.class);
		assertThat(single.apply(null),
			equalTo(CombinedTestModel.Impl.class));

		QueryFunction<Store, Class<?>> second =
			single.add(
				QueryFunction.single(CombinedTestModel.Controller.class));
		assertThat(second.apply(null),
			equalTo(CombinedTestModel.Impl.class, CombinedTestModel.Controller.class));
	}

	@Test
	public void getAllQuery() {
		QueryFunction<Store, Class<?>> single =
			QueryFunction.single(CombinedTestModel.Impl.class);

		QueryFunction<Store, Class<?>> allIncluding =
			single.add(
				single.getAll(SuperTypes::get));
		assertThat(allIncluding.apply(null),
			equalTo(CombinedTestModel.Impl.class, CombinedTestModel.Abstract.class, CombinedTestModel.Controller.class));
	}

	@Test
	public void flatMapQuery() throws NoSuchMethodException {
		Set<Method> query =
			get(Annotations.of(
					Methods.of(CombinedTestModel.Impl.class))
				.flatMap(annotation ->
					Methods.of(annotation.annotationType())));

		Set<Method> query1 =
			get(AnnotationTypes.of(Methods.of(CombinedTestModel.Impl.class)).flatMap(Methods::of));

		assertThat(query,
			equalTo(
				CombinedTestModel.Post.class.getDeclaredMethod("value"),
				CombinedTestModel.Requests.class.getDeclaredMethod("value"),
				CombinedTestModel.Get.class.getDeclaredMethod("value"),
				Annotation.class.getDeclaredMethod("annotationType")));

		assertEquals(query, query1);
	}

	@Test
	public void annotationToMap() {
		Set<Map<String, Object>> valueMaps =
			get(Annotations.of(
					Methods.of(CombinedTestModel.Impl.class))
				.map(ReflectionUtils::toMap));

		// todo proper assert
		Set<String> collect = valueMaps.stream().map(Object::toString).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
		assertThat(collect,
			equalTo(
				"{annotationType=interface org.reflections.CombinedTestModel$Get, value=/get}",
				"{annotationType=interface org.reflections.CombinedTestModel$Post, value=/post}",
				"{annotationType=interface org.reflections.CombinedTestModel$Requests, value=[" +
					"{method=PUT, annotationType=interface org.reflections.CombinedTestModel$Request, value=/another}, " +
					"{method=PATCH, annotationType=interface org.reflections.CombinedTestModel$Request, value=/another}]}"
			));
	}

	@Test
	public void mergedAnnotations() {
		Class<CombinedTestModel.Request> metaAnnotation = CombinedTestModel.Request.class;

		Reflections reflections = new Reflections(metaAnnotation, Scanners.values());

		Set<Class<?>> metaAnnotations =
			reflections.get(TypesAnnotated.getAllIncluding(metaAnnotation.getName()).asClass());

		QueryFunction<Store, CombinedTestModel.Request> mergedAnnotations =
			MethodsAnnotated.with(metaAnnotations)
				.as(Method.class)
				.map(method ->
					get(Annotations.of(method.getDeclaringClass())
						.add(Annotations.of(method))
						.filter(a -> metaAnnotations.contains(a.annotationType())))
						.stream()
						.collect(new AnnotationMergeCollector(method)))
				.map(map -> ReflectionUtils.toAnnotation(map, metaAnnotation));

		assertThat(
			reflections.get(mergedAnnotations.map(CombinedTestModel.Request::value)),
			equalTo("/base/post", "/base/get"));

		assertThat(
			reflections.get(mergedAnnotations.map(CombinedTestModel.Request::method)),
			equalTo("Post", "Get"));
	}
}

