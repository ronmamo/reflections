package org.reflections.scanners;

import javassist.bytecode.ClassFile;
import org.reflections.vfs.Vfs;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scanner {@link #scan(ClassFile)} method receives a {@link ClassFile} and produce a list of {@link Map.Entry}.
 * These key/values will be stored under {@link #index()} for querying.
 * <br><br>see more in {@link Scanners}
 * */
public interface Scanner {

    /** scan the given {@code classFile} and produces list of {@link Map.Entry} key/values */
    List<Map.Entry<String, String>> scan(ClassFile classFile);

    /** scan the given {@code file} and produces list of {@link Map.Entry} key/values */
    @Nullable
    default List<Map.Entry<String, String>> scan(Vfs.File file) {
        return null;
    }

    /** unique index name for scanner */
    default String index() {
        return getClass().getSimpleName();
    }

    default boolean acceptsInput(String file) {
        return file.endsWith(".class");
    }

    default Map.Entry<String, String> entry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    default List<Map.Entry<String, String>> entries(Collection<String> keys, String value) {
        return keys.stream().map(key -> entry(key, value)).collect(Collectors.toList());
    }

    default List<Map.Entry<String, String>> entries(String key, String value) {
        return Collections.singletonList(entry(key, value));
    }

    default List<Map.Entry<String, String>> entries(String key, Collection<String> values) {
        return values.stream().map(value -> entry(key, value)).collect(Collectors.toList());
    }
}
