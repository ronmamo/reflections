package org.reflections.log;

public interface Logger {

    void debug(String string, Throwable e);
    void warn(String string, Throwable e);
    void error(String string, Throwable e);

    void debug(String string);
    void info(String string);
    void warn(String string);

    boolean isDebugEnabled();
    boolean isWarnEnabled();
}
