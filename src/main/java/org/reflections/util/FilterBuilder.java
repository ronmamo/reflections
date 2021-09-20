package org.reflections.util;

import org.reflections.ReflectionsException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * include/exclude filter builder
 * <pre>{@code
 * new FilterBuilder().includePackage("java").excludePackage("java.lang")
 *
 * FilterBuilder.parsePackages("+java, -java.lang")
 *
 * new FilterBuilder().includePattern("java\\..*").excludePackage("java\\.lang\\..*")
 * }</pre>
 * <i>note that includePackage/excludePackage value is mapped into a prefix pattern with a trailing dot, for example: {@code includePackage("a.b")} is equivalent to {@code includePattern("a\\.b\\..*)}
 * </pre>
 */
public class FilterBuilder implements Predicate<String> {
    private final List<Predicate<String>> chain = new ArrayList<>();

    public FilterBuilder() {}

	private FilterBuilder(Collection<Predicate<String>> filters) {
        chain.addAll(filters);
    }

    /** include package prefix <pre>{@code new FilterBuilder().includePackage("java.lang")}</pre>
     * <i>note that the {@code value} is mapped into a prefix pattern with a trailing dot, for example {@code "a.b" == "a\\.b\\..*}
     * <p>see more in {@link #prefixPattern(String)} */
    public FilterBuilder includePackage(String value) {
        return includePattern(prefixPattern(value));
    }

    /** exclude package prefix <pre>{@code new FilterBuilder().excludePackage("java.lang")}</pre>
     * <i>note that the {@code value} is mapped into a prefix pattern with a trailing dot, for example {@code "a.b" == "a\\.b\\..*}
     * <p>see more in {@link #prefixPattern(String)} */
    public FilterBuilder excludePackage(String value) {
        return excludePattern(prefixPattern(value));
    }

    /** include regular expression <pre>{@code new FilterBuilder().includePattern("java\\.lang\\..*")}</pre>
     * see also {@link #includePackage(String)}*/
    public FilterBuilder includePattern(String regex) {
        return add(new FilterBuilder.Include(regex));
    }

    /** exclude regular expression <pre>{@code new FilterBuilder().excludePattern("java\\.lang\\..*")}</pre>
     * see also {@link #excludePackage(String)}*/
    public FilterBuilder excludePattern(String regex) {
        return add(new FilterBuilder.Exclude(regex));
    }

    /** include a regular expression <p>deprecated, use {@link #includePattern(String)} */
    @Deprecated
    public FilterBuilder include(String regex) {
        return add(new Include(regex));
    }

    /** exclude a regular expression <p>deprecated, use {@link #excludePattern(String)} */
    @Deprecated
    public FilterBuilder exclude(String regex) {
        add(new Exclude(regex)); return this;
    }

    /**
     * Parses csv of include/exclude package prefix filter, where each value starting with +/-
     * <pre>{@code FilterBuilder.parsePackages("-java, -javax, +java.util")}</pre>
     * each value is mapped into a prefix pattern with a trailing dot, for example {@code "a.b" == "a\\.b\\..*}. see more in {@link #prefixPattern(String)}
     */
    public static FilterBuilder parsePackages(String includeExcludeString) {
        List<Predicate<String>> filters = new ArrayList<>();
        for (String string : includeExcludeString.split(",")) {
            String trimmed = string.trim();
            char prefix = trimmed.charAt(0);
            String pattern = prefixPattern(trimmed.substring(1));
            switch (prefix) {
                case '+': filters.add(new Include(pattern)); break;
                case '-': filters.add(new Exclude(pattern)); break;
                default: throw new ReflectionsException("includeExclude should start with either + or -");
            }
        }
        return new FilterBuilder(filters);
    }

    public FilterBuilder add(Predicate<String> filter) {
        chain.add(filter);
        return this;
    }

    public boolean test(String regex) {
        boolean accept = chain.isEmpty() || chain.get(0) instanceof Exclude;

        for (Predicate<String> filter : chain) {
            if (accept && filter instanceof Include) {continue;} //skip if this filter won't change
            if (!accept && filter instanceof Exclude) {continue;}
            accept = filter.test(regex);
            if (!accept && filter instanceof Exclude) {break;} //break on first exclusion
        }
        return accept;
    }

    @Override public String toString() {
        return chain.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    /** maps fqn to prefix pattern with a trailing dot, for example {@code packageNamePrefix("a.b") == "a\\.b\\..*} */
    private static String prefixPattern(String fqn) {
        if (!fqn.endsWith(".")) fqn += ".";
        return fqn.replace(".", "\\.").replace("$", "\\$") + ".*";
    }

    abstract static class Matcher implements Predicate<String> {
        final Pattern pattern;
        Matcher(final String regex) {pattern = Pattern.compile(regex);}
        @Override public String toString() {return pattern.pattern();}
    }

    static class Include implements Predicate<String> {
        final Pattern pattern;
        Include(final String regex) {pattern = Pattern.compile(regex);}
        @Override public boolean test(final String regex) {return pattern.matcher(regex).matches();}
        @Override public String toString() {return "+" + pattern;}
    }

    static class Exclude implements Predicate<String> {
        final Pattern pattern;
        Exclude(final String regex) {pattern = Pattern.compile(regex);}
        @Override public boolean test(final String regex) {return !pattern.matcher(regex).matches();}
        @Override public String toString() {return "-" + pattern;}
    }
}
