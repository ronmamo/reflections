package org.reflections.util;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Sets {
    public static Set<String> difference(final Set<String> set1, final Set<String> set2) {
        return set1.stream().filter(e -> !set2.contains(e)).collect(Collectors.toSet());
    }

    public static <T> Set<T> newSet(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toSet());
    }
}
