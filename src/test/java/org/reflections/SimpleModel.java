package org.reflections;

public interface SimpleModel {
    public interface SimpleSuperInterface { void method();}
    public interface SimpleChildInterface extends SimpleSuperInterface {void method2();}

    public class SimpleSuperClass { void method(){}}
    public class SimpleChildClass extends SimpleSuperClass {void method2(){}}
}
