package org.reflections;

import org.reflections.util.Multimap;
import org.reflections.util.SetMultimap;
import org.reflections.util.SynchronizedSetMultimap;
import org.reflections.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * stores metadata information in multimaps
 * <p>use the different query methods (getXXX) to query the metadata
 * <p>the query methods are string based, and does not cause the class loader to define the types
 * <p>use {@link org.reflections.Reflections#getStore()} to access this store
 */
public class Store {

    private transient boolean concurrent;
    private final Map<String, Multimap<String, String>> storeMap;

    //used via reflection
    @SuppressWarnings("UnusedDeclaration")
    protected Store() {
        storeMap = new HashMap<String, Multimap<String, String>>();
        concurrent = false;
    }

    public Store(Configuration configuration) {
        storeMap = new HashMap<String, Multimap<String, String>>();
        concurrent = configuration.getExecutorService() != null;
    }

    /** return all indices */
    public Set<String> keySet() {
        return storeMap.keySet();
    }

    /** get or create the multimap object for the given {@code index} */
    public Multimap<String, String> getOrCreate(String index) {
        Multimap<String, String> mmap = storeMap.get(index);
        if (mmap == null) {
            SetMultimap<String, String> multimap =
                    new SetMultimap<>();
            mmap = concurrent ? new SynchronizedSetMultimap<>(multimap) : multimap;
            storeMap.put(index,mmap);
        }
        return mmap;
    }

    /** get the multimap object for the given {@code index}, otherwise throws a {@link org.reflections.ReflectionsException} */
    public Multimap<String, String> get(String index) {
        Multimap<String, String> mmap = storeMap.get(index);
        if (mmap == null) {
            throw new ReflectionsException("Scanner " + index + " was not configured");
        }
        return mmap;
    }

    /** get the values stored for the given {@code index} and {@code keys} */
    public Iterable<String> get(String index, String... keys) {
        return get(index, Arrays.asList(keys));
    }

    /** get the values stored for the given {@code index} and {@code keys} */
    public Iterable<String> get(String index, Iterable<String> keys) {
        Multimap<String, String> mmap = get(index);
        IterableChain<String> result = new IterableChain<String>();
        for (String key : keys) {
            result.addAll(mmap.get(key));
        }
        return result;
    }

    /** recursively get the values stored for the given {@code index} and {@code keys}, including keys */
    private Iterable<String> getAllIncluding(String index, Iterable<String> keys, IterableChain<String> result) {
        result.addAll(keys);
        for (String key : keys) {
            Iterable<String> values = get(index, key);
            if (values.iterator().hasNext()) {
                getAllIncluding(index, values, result);
            }
        }
        return result;
    }

    /** recursively get the values stored for the given {@code index} and {@code keys}, not including keys */
    public Iterable<String> getAll(String index, String key) {
        return getAllIncluding(index, get(index, key), new IterableChain<String>());
    }

    /** recursively get the values stored for the given {@code index} and {@code keys}, not including keys */
    public Iterable<String> getAll(String index, Iterable<String> keys) {
        return getAllIncluding(index, get(index, keys), new IterableChain<String>());
    }

    public int getKeySize() {
        return 0;//TODO
    }

    public int getValueSize() {
        return 0;//TODO
    }

    private static class IterableChain<T> implements Iterable<T> {
        private final List<Iterable<T>> chain = new ArrayList<>();

        private void addAll(Iterable<T> iterable) { chain.add(iterable); }

        public Iterator<T> iterator() { return Utils.concat(chain).iterator(); }
    }
}
