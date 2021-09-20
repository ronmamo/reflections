package org.reflections.scanners;

/** scan field annotations.
 * <i>{@code Deprecated}, use {@link Scanners#FieldsAnnotated} instead</i>
 * */
@Deprecated
public class FieldAnnotationsScanner extends AbstractScanner {

    public FieldAnnotationsScanner() {
        super(Scanners.FieldsAnnotated);
    }
}
