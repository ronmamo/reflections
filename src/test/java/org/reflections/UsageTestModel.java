package org.reflections;

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
	}
}
