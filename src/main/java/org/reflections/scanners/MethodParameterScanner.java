package org.reflections.scanners;

/** Not supported since 0.10, will be removed.
 * <p></p><i>{@code Deprecated}, use instead:
 * <ul>
 *  <li>{@link Scanners#MethodsParameter}</li>
 *  <li>{@link Scanners#MethodsSignature}</li>
 *  <li>{@link Scanners#MethodsReturn}</li>
 *  <li>{@link Scanners#ConstructorsParameter}</li>
 *  <li>{@link Scanners#ConstructorsSignature}</li>
 * </ul>
 * */
@Deprecated
public class MethodParameterScanner extends AbstractScanner {

    /** Not supported since 0.10, will be removed.
     * <p></p><i>{@code Deprecated}, use instead:
     * <ul>
     *  <li>{@link Scanners#MethodsParameter}</li>
     *  <li>{@link Scanners#MethodsSignature}</li>
     *  <li>{@link Scanners#MethodsReturn}</li>
     *  <li>{@link Scanners#ConstructorsParameter}</li>
     *  <li>{@link Scanners#ConstructorsSignature}</li>
     * </ul>
     */
    @Deprecated
    public MethodParameterScanner() {
        super(Scanners.MethodsParameter);
    }
}
