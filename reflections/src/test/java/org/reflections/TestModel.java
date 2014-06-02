package org.reflections;

import java.lang.annotation.Retention;
import java.lang.annotation.Inherited;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 */
@SuppressWarnings({"ALL"})
public interface TestModel {
    public @Retention(RUNTIME) @Inherited @interface MAI1 {}
    public @Retention(RUNTIME) @MAI1 @interface AI1 {}
    public @AI1 interface I1 {}
    public @Retention(RUNTIME) @Inherited @interface AI2 {}
    public @AI2 interface I2 extends I1 {}

    public @Retention(RUNTIME) @Inherited @interface AC1 {}
    public @Retention(RUNTIME) @interface AC1n {}
    public @AC1 @AC1n class C1 implements I2 {}
    public @Retention(RUNTIME) @interface AC2 {
        public abstract String value();
    }

    public @AC2("grr...") class C2 extends C1 {}
    public @AC2("ugh?!") class C3 extends C1 {}

    public @Retention(RUNTIME) @interface AM1 {
        public abstract String value();
    }
    public @Retention(RUNTIME) @interface AF1 {
        public abstract String value();
    }
    public class C4 {
        @AF1("1") private String f1;
        @AF1("2") protected String f2;
        protected String f3;

        public C4() { }
        @AM1("1") public C4(@AM1("1") String f1) { this.f1 = f1; }

        @AM1("1") protected void m1() {}
        @AM1("1") public void m1(int integer, String... strings) {}
        @AM1("1") public void m1(int[][] integer, String[][] strings) {}
        @AM1("2") public String m3() {return null;}
        public String m4(@AM1("2") String string) {return null;}
        public C3 c2toC3(C2 c2) {return null;}
        public int add(int i1, int i2) { return i1+i2; }
    }
    
    public class C5 extends C3 {}
    public @AC2("ugh?!") interface I3 {}
    public class C6 implements I3 {}

    public @Retention(RUNTIME) @AC2("ugh?!") @interface AC3 { }
    public @AC3 class C7 {}

    public interface Usage {
        public static class C1 {
            C2 c2 = new C2();
            public C1() { }
            public C1(C2 c2) { this.c2 = c2; }
            public void method() { c2.method(); }
            public void method(String string) { c2.method(); }
        }
        public static class C2 {
            C1 c1 = new C1();
            public void method() {
                c1 = new C1();
                c1 = new C1(this);
                c1.method();
                c1.method("");
            }
        }
    }
}
