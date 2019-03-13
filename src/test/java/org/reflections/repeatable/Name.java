package org.reflections.repeatable;

import java.lang.annotation.*;

@Repeatable(Names.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Name {
    String name();
}
