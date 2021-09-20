package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.util.FilterBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilterBuilderTest {

    @Test
    public void includeExcludePackage() {
        FilterBuilder filter = new FilterBuilder()
            .includePackage("org.reflections")
            .excludePackage("org.reflections.exclude")
            .includePackage("org.foo");

        doAssert(filter);
    }

    @Test
    public void parsePackages() {
        FilterBuilder filter = FilterBuilder
            .parsePackages("+org.reflections ,  -org.reflections.exclude,+org.foo"); // not trimmed

        doAssert(filter);
    }

    @Test
    public void includeExcludePattern() {
        FilterBuilder filter = new FilterBuilder()
            .includePattern("org\\.reflections\\..*")
            .excludePattern("org\\.reflections\\.exclude\\..*")
            .includePattern("org\\.foo\\..*");

        doAssert(filter);
    }

    private void doAssert(FilterBuilder filter) {
        assertFalse(filter.test(""));
        assertFalse(filter.test("org"));
        assertFalse(filter.test("org."));
        assertFalse(filter.test("org.reflections"));
        assertTrue(filter.test("org.reflections."));
        assertTrue(filter.test("org.reflections.Reflections"));
        assertTrue(filter.test("org.reflections.foo.Reflections"));
        assertFalse(filter.test("org.reflections.exclude.it"));
        assertFalse(filter.test("org.foo"));
        assertTrue(filter.test("org.foo."));
        assertTrue(filter.test("org.foo.bar"));
        assertFalse(filter.test("org.bar.Reflections"));
    }

}
