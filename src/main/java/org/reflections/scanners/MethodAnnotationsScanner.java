package org.reflections.scanners;

/** scan method annotations.
 * <p></p><i>breaking change: does not include constructor annotations, use {@link Scanners#ConstructorsAnnotated} instead </i>
 * <p></p><i>{@code Deprecated}, use {@link Scanners#MethodsAnnotated} and {@link Scanners#ConstructorsAnnotated} instead</i> */
@Deprecated
public class MethodAnnotationsScanner extends AbstractScanner {

    /** <i>{@code Deprecated}, use {@link Scanners#MethodsAnnotated} and {@link Scanners#ConstructorsAnnotated} instead</i> */
    @Deprecated
    public MethodAnnotationsScanner() {
        super(Scanners.MethodsAnnotated);
    }
}
