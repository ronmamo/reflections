package org.reflections.util;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class NamedThreadFactory implements ThreadFactory {
    private final ThreadFactory backingThreadFactory = Executors.defaultThreadFactory();
    private final String nameFormat;
    private final boolean service;
    private final AtomicLong count = new AtomicLong(0);

    public NamedThreadFactory(final String nameFormat, final boolean service) {
        this.nameFormat = nameFormat;
        this.service = service;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = backingThreadFactory.newThread(runnable);
        thread.setName(format(nameFormat, count.getAndIncrement()));
        thread.setDaemon(service);
        return thread;
    }

    private static String format(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }
}
