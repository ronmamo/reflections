package org.reflections;

import org.junit.Test;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class ReflectionsThreadSafenessTest {

    /**
     * https://github.com/ronmamo/reflections/issues/81
     */
    @Test
    public void reflections_scan_is_thread_safe() throws Exception {

        Callable<Set<Class<? extends Map>>> callable = new Callable<Set<Class<? extends Map>>>() {
            @Override
            public Set<Class<? extends Map>> call() throws Exception {
                final Reflections reflections = new Reflections(new ConfigurationBuilder()
                        .setUrls(singletonList(ClasspathHelper.forClass(Map.class)))
                        .setScanners(new SubTypesScanner(false)));

                return reflections.getSubTypesOf(Map.class);
            }
        };

        final ExecutorService pool = Executors.newFixedThreadPool(2);

        final Future<?> first = pool.submit(callable);
        final Future<?> second = pool.submit(callable);

        assertEquals(first.get(15, SECONDS), second.get(15, SECONDS));
    }
}
