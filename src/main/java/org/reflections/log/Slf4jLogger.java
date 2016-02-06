package org.reflections.log;

import org.slf4j.LoggerFactory;

public class Slf4jLogger implements Logger {
    private org.slf4j.Logger logger;

    public Slf4jLogger(Class<?> aClass) {
        this.logger =  LoggerFactory.getLogger(aClass);
    }

    @Override
    public void debug(String string, Throwable e) {
        logger.debug(string, e);
    }

    @Override
    public void warn(String string, Throwable e) {
        logger.warn(string, e);
    }

    @Override
    public void error(String string, Throwable e) {
        logger.error(string, e);
    }

    @Override
    public void debug(String string) {
        logger.debug(string);
    }

    @Override
    public void info(String string) {
        logger.info(string);
    }

    @Override
    public void warn(String string) {
        logger.warn(string);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

}
