package org.reflections.scanners;

import com.google.common.base.Joiner;

/** scans fields and methods and stores fqn as key and elements as values */
@SuppressWarnings({"unchecked"})
public class TypeElementsScanner extends AbstractScanner {
    private boolean includeFields = true;
    private boolean includeMethods = true;
    private boolean includeAnnotations = true;
    private boolean publicOnly = true;

    public void scan(Object cls) {
        String className = getMetadataAdapter().getClassName(cls);
        if (!acceptResult(className)) return;

        getStore().put(className, "");

        if (includeFields) {
            for (Object field : getMetadataAdapter().getFields(cls)) {
                String fieldName = getMetadataAdapter().getFieldName(field);
                getStore().put(className, fieldName);
            }
        }

        if (includeMethods) {
            for (Object method : getMetadataAdapter().getMethods(cls)) {
                if (!publicOnly || getMetadataAdapter().isPublic(method)) {
                    String methodKey = getMetadataAdapter().getMethodName(method) + "(" +
                            Joiner.on(", ").join(getMetadataAdapter().getParameterNames(method)) + ")";
                    getStore().put(className, methodKey);
                }
            }
        }

        if (includeAnnotations) {
            for (Object annotation : getMetadataAdapter().getClassAnnotationNames(cls)) {
                getStore().put(className, "@" + annotation);
            }
        }
    }

    //
    public TypeElementsScanner includeFields() { return includeFields(true); }
    public TypeElementsScanner includeFields(boolean include) { includeFields = include; return this; }
    public TypeElementsScanner includeMethods() { return includeMethods(true); }
    public TypeElementsScanner includeMethods(boolean include) { includeMethods = include; return this; }
    public TypeElementsScanner includeAnnotations() { return includeAnnotations(true); }
    public TypeElementsScanner includeAnnotations(boolean include) { includeAnnotations = include; return this; }
    public TypeElementsScanner publicOnly(boolean only) { publicOnly = only; return this; }
    public TypeElementsScanner publicOnly() { return publicOnly(true); }
}
