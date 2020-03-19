package org.reflections.scanners;

import org.reflections.Store;

import java.util.List;

@SuppressWarnings({"unchecked"})
/** scans for method's annotations */
public class MethodAnnotationsScanner extends AbstractScanner {

    public MethodAnnotationsScanner() {
        this.enableMetaAnnotations = false;
    }

    public MethodAnnotationsScanner(boolean enableMetaAnnotations) {
        this.enableMetaAnnotations = enableMetaAnnotations;
    }

    private final boolean enableMetaAnnotations;

    public void scan(final Object cls, Store store) {
        for (Object method : getMetadataAdapter().getMethods(cls)) {
            List<String> methodAnnotations = enableMetaAnnotations? getMetadataAdapter().getMetaMethodAnnotationNames(method) : getMetadataAdapter().getMethodAnnotationNames(method);

            for (String methodAnnotation : methodAnnotations) {
                if (acceptResult(methodAnnotation)) {
                    put(store, methodAnnotation, getMetadataAdapter().getMethodFullKey(cls, method));
                }
            }
        }
    }
}
