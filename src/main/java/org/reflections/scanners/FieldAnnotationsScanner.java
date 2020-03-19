package org.reflections.scanners;

import org.reflections.Store;

import java.util.List;

/** scans for field's annotations */
@SuppressWarnings({"unchecked"})
public class FieldAnnotationsScanner extends AbstractScanner {

    public FieldAnnotationsScanner() {
        this.enableMetaAnnotations = false;
    }

    public FieldAnnotationsScanner(boolean enableMetaAnnotations) {
        this.enableMetaAnnotations = enableMetaAnnotations;
    }

    private final boolean enableMetaAnnotations;

    public void scan(final Object cls, Store store) {
        final String className = getMetadataAdapter().getClassName(cls);
        List<Object> fields = getMetadataAdapter().getFields(cls);
        for (final Object field : fields) {
            List<String> fieldAnnotations = enableMetaAnnotations ? getMetadataAdapter().getMetaFieldAnnotationNames(field) : getMetadataAdapter().getFieldAnnotationNames(field);
            for (String fieldAnnotation : fieldAnnotations) {

                if (acceptResult(fieldAnnotation)) {
                    String fieldName = getMetadataAdapter().getFieldName(field);
                    put(store, fieldAnnotation, String.format("%s.%s", className, fieldName));
                }
            }
        }
    }
}
