package org.reflections.scanners;

import javassist.bytecode.ClassFile;

import java.util.List;
import java.util.Map;

/** scan superclass and interfaces of a class, allowing a reverse lookup for subtypes.
 * <i>{@code Deprecated}, use {@link Scanners#SubTypes} instead</i>
 * */
@Deprecated
public class SubTypesScanner extends AbstractScanner {

    /** create new SubTypesScanner. will exclude direct Object subtypes */
    public SubTypesScanner() {
        super(Scanners.SubTypes);
    }

    /** create new SubTypesScanner. include direct {@link Object} subtypes in results.  */
    public SubTypesScanner(boolean excludeObjectClass) {
        super(excludeObjectClass ? Scanners.SubTypes : Scanners.SubTypes.filterResultsBy(s -> true));
    }

    public List<Map.Entry<String, String>> scan(final ClassFile cls) {
        return scanner.scan(cls);
    }
}
