package org.reflections.scanners;

/** scan method annotations.
 * <i>{@code Deprecated}, use {@link Scanners#MethodsAnnotated} instead</i>
 * */
@Deprecated
public class MethodAnnotationsScanner extends AbstractScanner {

    public MethodAnnotationsScanner() {
        super(Scanners.MethodsAnnotated);
    }
}
