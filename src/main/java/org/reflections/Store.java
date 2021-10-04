package org.reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** stores string key/value pairs per scanner index in a multimap {@code Map<String, Map<String, Set<String>>>}
 * <pre>{@code Set<String> values = reflections.getStore().get("index").get("key")}</pre>
 * <i>{@code Store} multimap is not copy protected, preferably use {@link org.reflections.util.QueryBuilder} to safely rich query the metadata </i> */
public class Store extends HashMap<String, Map<String, Set<String>>> {
    public Store() {}
    public Store(Map<String, Map<String, Set<String>>> storeMap) { super(storeMap); }
}
