package org.reflections;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

interface CombinedTestModel {
	@Retention(RUNTIME)
	@interface Alias { String value();}

	@Retention(RUNTIME)
	@interface Requests {
		Request[] value();
	}

	@Retention(RUNTIME)
	@Repeatable(Requests.class)
	@interface Request {
		@Alias("path") String value() default "";
		String method() default "";
	}

	@Retention(RUNTIME) @Request(method = "Get")
	@interface Get { String value();}

	@Retention(RUNTIME) @Request(method = "Post")
	@interface Post { String value();}


	@Request("/base")
	interface Controller {
		@Get("/get") void get();
		@Post("/post")
		void post(Object object);
	}

	abstract class Abstract implements Controller {
		@Override public void get() {}
	}

	class Impl extends Abstract {
		@Requests({@Request(method = "PUT", value = "/another"),
			@Request(method = "PATCH", value = "/another")})
		@Override public void post(Object object) {}
	}
}
