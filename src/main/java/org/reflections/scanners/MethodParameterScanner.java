package org.reflections.scanners;

/** scan methods/constructors and indexes parameters, return type and parameter annotations.
 * <i>{@code Deprecated}, use {@link Scanners#MethodsParameter} instead</i>
 * */
@Deprecated
public class MethodParameterScanner extends AbstractScanner {

    public MethodParameterScanner() {
        super(Scanners.MethodsParameter);
    }
}
