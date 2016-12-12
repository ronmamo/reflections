//        DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
//                    Version 2, December 2004 
//
// Copyright (C) 2004 Sam Hocevar <sam@hocevar.net> 
//
// Everyone is permitted to copy and distribute verbatim or modified 
// copies of this license document, and changing it is allowed as long 
// as the name is changed. 
//
//            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
//   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION 
//
//  0. You just DO WHAT THE FUCK YOU WANT TO.
package org.reflections.scanners;

import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.scanners.AbstractScanner;

import javassist.ClassPool;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

/**
 * JunitCategoryScanner is a scanner for reflections framework. The goal of this scanner is to list TestCase classes and
 * Test methods per Category. Test methods are methods with @Test annotation and TestCase classes are classes with Test
 * methods.<br/>
 * In generated Store, TestCase and Test method are grouped by category and are duplicated for each category there are
 * associated to.<br/>
 * Category can be filtered with Predicate&lt;String&gt;
 *
 * @author abourree
 */
public class JUnitCategoryScanner extends AbstractScanner {

    /**
     * Check if provided full qualified name is @Test one
     *
     * @param fqn The full qualified name of the annotation to check
     * @return true if annotation is Test full qualified name
     */
    public boolean isTestAnnotation(final String fqn) {
        return Test.class.getCanonicalName().equals(fqn);
    }

    /**
     * Check if provided full qualified name is @Category one
     *
     * @param fqn The full qualified name of the annotation to check
     * @return true if annotation is Category full qualified name
     */
    public boolean isCategoryAnnotation(final String fqn) {
        return Category.class.getCanonicalName().equals(fqn);
    }

    /**
     * Store full qualified name for all category values
     *
     * @param annotation The @Category instance
     * @param fqn To associate to each @Category values
     * @return true if at least one category has been added
     */
    public boolean multiStore(final Category annotation, final String fqn) {
        boolean hasCategoryAdded = false;
        for (Class category : annotation.value()) {
            final String categoryName = category.getCanonicalName();
            if (acceptResult(categoryName)) {
                hasCategoryAdded = true;
                getStore().put(categoryName, fqn);
            }
        }
        return hasCategoryAdded;
    }

    /**
     * Get @Category annotation instance from a class
     *
     * @param cls The class to fetch @Category from
     * @return @Category annotation instance
     * @throws ClassNotFoundException if @Category is not in class-path
     */
    public Category getCategoryFromClass(Object cls) throws ClassNotFoundException {
        ClassFile info = (ClassFile) cls;
        AnnotationsAttribute attribute = (AnnotationsAttribute) info.getAttribute(AnnotationsAttribute.visibleTag);
        javassist.bytecode.annotation.Annotation annotation = attribute.getAnnotation(
                Category.class.getCanonicalName());
        return (Category) annotation.toAnnotationType(Category.class.getClassLoader(), new ClassPool());
    }

    /**
     * Get @Category annotation instance from method
     *
     * @param method The method to fetch @Category from
     * @return @Category annotation instance
     * @throws ClassNotFoundException if @Category is not in class-path
     */
    public Category getCategoryFromMethod(Object method) throws ClassNotFoundException {
        MethodInfo info = (MethodInfo) method;
        AnnotationsAttribute attribute = (AnnotationsAttribute) info.getAttribute(AnnotationsAttribute.visibleTag);
        javassist.bytecode.annotation.Annotation annotation = attribute.getAnnotation(
                Category.class.getCanonicalName());
        return (Category) annotation.toAnnotationType(Category.class.getClassLoader(), new ClassPool());
    }

    /**
     * Main scanner method call on reflexion.<br/>
     *
     * @param cls
     */
    @Override
    public void scan(Object cls) {
        final String className = getMetadataAdapter().getClassName(cls);
        boolean hasTest = false;
        // Process @Test method 1st
        for (Object method : getMetadataAdapter().getMethods(cls)) {
            for (String methodAnnotation : (List<String>) getMetadataAdapter().getMethodAnnotationNames(method)) {
                // @Test annotation
                if (isTestAnnotation(methodAnnotation)) {
                    hasTest = true;
                }
                // @Category annotation
                if (isCategoryAnnotation(methodAnnotation)) {
                    try {
                        multiStore(getCategoryFromMethod(method), getMetadataAdapter().getMethodFullKey(cls, method));
                    } catch (ClassNotFoundException ex) {
                        ; //NOPMD
                    }
                }
            }
        }
        // If class has @Test method, then get @Category on class
        if (hasTest) {
            boolean hasCategoryAdded = false;
            for (String annotationType : (List<String>) getMetadataAdapter().getClassAnnotationNames(cls)) {
                if (isCategoryAnnotation(annotationType)) {
                    try {
                        hasCategoryAdded |= multiStore(getCategoryFromClass(cls), className);
                    } catch (ClassNotFoundException ex) {
                        ; //NOPMD
                    }
                }
            }
            if (!hasCategoryAdded) {
                getStore().put("WithOutCategory", className);
            }
        }
    }

}
