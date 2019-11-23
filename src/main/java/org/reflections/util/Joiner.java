package org.reflections.util;

public class Joiner {

    private final String separator;

    private Joiner(final String separator) {
        this.separator = separator;
    }

    public static Joiner on(final String separator) {
        return new Joiner(separator);
    }

    public String join(final String... strings) {
        return String.join(separator, strings);
    }

    public String join(final Iterable<String> strings) {
        return String.join(separator, strings);
    }
}
