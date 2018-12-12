package com.tvd12.reflections.testing;

import com.google.common.collect.ImmutableMap;
import com.tvd12.reflections.Reflections;
import com.tvd12.reflections.scanners.SubTypesScanner;
import com.tvd12.reflections.util.ClasspathHelper;
import com.tvd12.reflections.util.ConfigurationBuilder;

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class ReflectionsThreadSafenessTest {

    /**
     * https://github.com/ronmamo/reflections/issues/81
     */
    @Test
    public void reflections_scan_is_thread_safe() throws Exception {

		Callable<Set<Class<? extends ImmutableMap>>> callable = new Callable<Set<Class<? extends ImmutableMap>>>() {
            @Override
            public Set<Class<? extends ImmutableMap>> call() throws Exception {
                final Reflections reflections = new Reflections(new ConfigurationBuilder()
                        .setUrls(singletonList(ClasspathHelper.forClass(ImmutableMap.class)))
                        .setScanners(new SubTypesScanner(false)));

                return reflections.getSubTypesOf(ImmutableMap.class);
            }
        };

        final ExecutorService pool = Executors.newFixedThreadPool(2);

        final Future<?> first = pool.submit(callable);
        final Future<?> second = pool.submit(callable);

        assertEquals(first.get(5, SECONDS), second.get(5, SECONDS));
    }
}
