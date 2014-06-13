package org.reflections.scanners;

import java.util.List;

/** scans for field's annotations */
@SuppressWarnings({"unchecked"})
public class FieldAnnotationsScanner extends AbstractScanner {
    public void scan(final Object cls) {
        final String className = getMetadataAdapter().getClassName(cls);
        List<Object> fields = getMetadataAdapter().getFields(cls);
        for (final Object field : fields) {
            List<String> fieldAnnotations = getMetadataAdapter().getFieldAnnotationNames(field);
            for (String fieldAnnotation : fieldAnnotations) {

                if (acceptResult(fieldAnnotation)) {
                    String fieldName = getMetadataAdapter().getFieldName(field);
                    getStore().put(fieldAnnotation, String.format("%s.%s", className, fieldName));
                }
            }
        }
    }
}
