package junit.org.reflections;

import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReflectionsExpandSupertypesTest {

  private final static String packagePrefix =
      "junit.org.reflections.ReflectionsExpandSupertypesTest\\$TestModel\\$ScannedScope\\$.*";
  private FilterBuilder inputsFilter = new FilterBuilder().include(packagePrefix);

  @Test
  public void testExpandSupertypes() throws Exception {
    Reflections refExpand = new Reflections(new ConfigurationBuilder().
        setUrls(ClasspathHelper.forClass(TestModel.ScannedScope.C.class)).
        filterInputsBy(inputsFilter));
    assertTrue(refExpand.getConfiguration().shouldExpandSuperTypes());
    Set<Class<? extends TestModel.A>> subTypesOf = refExpand.getSubTypesOf(TestModel.A.class);
    assertTrue("expanded", subTypesOf.contains(TestModel.B.class));
    assertTrue("transitivity", subTypesOf.containsAll(refExpand.getSubTypesOf(TestModel.B.class)));
  }

  @Test
  public void testNotExpandSupertypes() throws Exception {
    Reflections refDontExpand = new Reflections(new ConfigurationBuilder().
        setUrls(ClasspathHelper.forClass(TestModel.ScannedScope.C.class)).
        filterInputsBy(inputsFilter).
        setExpandSuperTypes(false));
    assertFalse(refDontExpand.getConfiguration().shouldExpandSuperTypes());
    Set<Class<? extends TestModel.A>> subTypesOf1 = refDontExpand.getSubTypesOf(TestModel.A.class);
    assertFalse(subTypesOf1.contains(TestModel.B.class));
  }

  public interface TestModel {
    interface A {
    } // outside of scanned scope

    interface B extends A {
    } // outside of scanned scope, but immediate supertype

    interface ScannedScope {
      interface C extends B {
      }

      interface D extends B {
      }
    }
  }
}
