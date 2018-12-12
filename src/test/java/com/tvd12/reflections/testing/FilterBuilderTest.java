package com.tvd12.reflections.testing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.tvd12.reflections.Reflections;
import com.tvd12.reflections.util.FilterBuilder;

/**
 * Test filtering
 */
public class FilterBuilderTest {

  @Test
  public void test_include() {
      FilterBuilder filter = new FilterBuilder().include("com.tvd12\\.reflections.*");
      assertTrue(filter.test("com.tvd12.reflections.Reflections"));
      assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
      assertFalse(filter.test("org.foobar.Reflections"));
  }

    @Test
    public void test_includePackage() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.tvd12.reflections");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_includePackageMultiple() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.tvd12.reflections", "org.foo");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foo.Reflections"));
        assertTrue(filter.test("org.foo.bar.Reflections"));
        assertFalse(filter.test("org.bar.Reflections"));
    }

    @Test
    public void test_includePackagebyClass() {
        FilterBuilder filter = new FilterBuilder().includePackage(Reflections.class);
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_exclude() {
        FilterBuilder filter = new FilterBuilder().exclude("com.tvd12\\.reflections.*");
        assertFalse(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_excludePackage() {
        FilterBuilder filter = new FilterBuilder().excludePackage("com.tvd12.reflections");
        assertFalse(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_excludePackageByClass() {
        FilterBuilder filter = new FilterBuilder().excludePackage(Reflections.class);
        assertFalse(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_include() {
        FilterBuilder filter = FilterBuilder.parse("+com.tvd12.reflections.*");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
        assertTrue(filter.test("com.tvd12.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_include_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("+com.tvd12.reflections");
        assertFalse(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_exclude() {
        FilterBuilder filter = FilterBuilder.parse("-com.tvd12.reflections.*");
        assertFalse(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("com.tvd12.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_exclude_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("-com.tvd12.reflections");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertTrue(filter.test("com.tvd12.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_include_exclude() {
        FilterBuilder filter = FilterBuilder.parse("+com.tvd12.reflections.*, -com.tvd12.reflections.foo.*");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parsePackages_include() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.tvd12.reflections");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("com.tvd12.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_include_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.tvd12.reflections.");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertTrue(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("com.tvd12.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("-com.tvd12.reflections");
        assertFalse(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertTrue(filter.test("com.tvd12.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("-com.tvd12.reflections.");
        assertFalse(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertTrue(filter.test("com.tvd12.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_include_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.tvd12.reflections, -com.tvd12.reflections.foo");
        assertTrue(filter.test("com.tvd12.reflections.Reflections"));
        assertFalse(filter.test("com.tvd12.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

}
