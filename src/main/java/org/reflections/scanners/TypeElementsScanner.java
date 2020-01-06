package org.reflections.scanners;

import org.reflections.Store;

import static org.reflections.util.Utils.join;

/** scans fields and methods and stores fqn as key and elements as values */
@SuppressWarnings({"unchecked"})
public class TypeElementsScanner extends AbstractScanner {
    private boolean includeFields = true;
    private boolean includeMethods = true;
    private boolean includeAnnotations = true;
    private boolean publicOnly = true;

    public void scan(Object cls, Store store) {
        String className = getMetadataAdapter().getClassName(cls);
        if (!acceptResult(className)) return;

        put(store, className, "");

        if (includeFields) {
            for (Object field : getMetadataAdapter().getFields(cls)) {
                String fieldName = getMetadataAdapter().getFieldName(field);
                put(store, className, fieldName);
            }
        }

        if (includeMethods) {
            for (Object method : getMetadataAdapter().getMethods(cls)) {
                if (!publicOnly || getMetadataAdapter().isPublic(method)) {
                    String methodKey = getMetadataAdapter().getMethodName(method) + "(" +
                            join(getMetadataAdapter().getParameterNames(method), ", ") + ")";
                    put(store, className, methodKey);
                }
            }
        }

        if (includeAnnotations) {
            for (Object annotation : getMetadataAdapter().getClassAnnotationNames(cls)) {
                put(store, className, "@" + annotation);
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
