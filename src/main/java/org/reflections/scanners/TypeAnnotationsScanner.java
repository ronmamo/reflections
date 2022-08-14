package org.reflections.scanners;

/** scan class annotations, where @Retention(RetentionPolicy.RUNTIME).
 * <i>{@code Deprecated}, use {@link Scanners#TypesAnnotated} instead</i> */
@Deprecated
public class TypeAnnotationsScanner extends AbstractScanner {

    /** <i>{@code Deprecated}, use {@link Scanners#TypesAnnotated} instead</i> */
    @Deprecated
     public TypeAnnotationsScanner() {
        super(Scanners.TypesAnnotated);
    }
}
