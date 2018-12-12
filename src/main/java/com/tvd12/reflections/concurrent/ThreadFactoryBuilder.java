package com.tvd12.reflections.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryBuilder {

	private boolean deamon;
	private String nameFormat;
	private static final AtomicInteger COUNTER = new AtomicInteger(0);
	
	public ThreadFactoryBuilder setDaemon(boolean deamon) {
		this.deamon = deamon;
		return this;
	}

	public ThreadFactoryBuilder setNameFormat(String string) {
		this.nameFormat = string;
		return this;
	}
	
	public ThreadFactory build() {
		return new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(deamon);
				thread.setName(String.format(nameFormat, COUNTER.incrementAndGet()));
				return thread;
			}
		};
	}

}
