package org.reflections.scanners;

/** collects all resources that are not classes in a collection
 * <p>key: value - {web.xml: WEB-INF/web.xml}</p>
 * <i>{@code Deprecated}, use {@link Scanners#Resources} instead</i>
 * */
@Deprecated
public class ResourcesScanner extends AbstractScanner {

    public ResourcesScanner() {
        super(Scanners.Resources);
    }
}
