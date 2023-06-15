package org.reflections;

import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

public interface UsageTestModel {
	class C1 {
		C2 c2 = new C2();
		public C1() {}
		public C1(C2 c2) {this.c2 = c2;}
		public void method() {c2.method();}
		public void method(String string) {c2.method();}
	}

	class C2 {
		C1 c1 = new C1();
		public void method() {
			c1 = new C1();
			c1 = new C1(this);
			c1.method();
			c1.method("");
		}

		public double useAnonymousClass(C2... objects) {
			return Stream.of(objects)
					.mapToDouble(new ToDoubleFunction<C2>() {
						@Override
						public double applyAsDouble(C2 c1) {
							return c1.zero();
						}
					})
					.sum();
		}
		public double useLambda(C2... objects) {
			return Stream.of(objects)
					.mapToDouble(it -> it.zero())
					.sum();
		}
		public double useMethodReference(C2... objects) {
			return Stream.of(objects)
					.mapToDouble(C2::zero)
					.sum();
		}
		double zero() { return 0; }
	}
}
