package org.reflections.log;

public class NullLogger implements Logger {
    @Override
    public void debug(String string, Throwable e) {
    }

    @Override
    public void warn(String string, Throwable e) {
    }

    @Override
    public void error(String string, Throwable e) {
    }

    @Override
    public void debug(String string) {
    }

    @Override
    public void info(String string) {
    }

    @Override
    public void warn(String string) {
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }
}
